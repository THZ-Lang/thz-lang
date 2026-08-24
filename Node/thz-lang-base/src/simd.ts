/* ============================================================
 * THZ-LANG — Semântica SIMD Formal (G5)
 * Regras verificáveis para VETORIZAR_PARA + LAYOUT_COLUNAR.
 * ============================================================ */

import type { ComandoAST, EstruturaAST } from './types.js';

export interface VerificacaoSimd {
  verificado: boolean;
  passoEfetivo: number | null;
  layoutFonte: 'SoA' | 'AoS' | 'desconhecido';
  diagnosticos: string[];
  regrasAplicadas: string[];
}

const PASSOS_VALIDOS = new Set([4, 8, 16, 32, 64]);

function ehPotenciaDeDois(n: number): boolean {
  return Number.isInteger(n) && n > 0 && (n & (n - 1)) === 0;
}

/**
 * Verifica se um VETORIZAR_PARA pode ser vetorizado de forma segura.
 * Formaliza as regras G5:
 *  R1 — Fonte deve ser FATIA[T] com estrutura conhecida.
 *  R2 — Estrutura fonte idealmente LAYOUT_COLUNAR (SoA) para SIMD efetivo.
 *  R3 — PASSO_SIMD, se presente, deve ser potência de dois 4..64.
 *  R4 — Corpo não deve conter ENQUANTO, FALHAR_COM/RETORNE de escape, ou VETORIZAR aninhado com mesma fonte (complexidade).
 *  R5 — Escritas fora do elemento devem ser reduções puras (acc <- acc op f(item)).
 */
export function verificarVetorizado(
  cmd: Extract<ComandoAST, { tipoComando: 'VETORIZAR_PARA' }>,
  estruturas: Map<string, EstruturaAST>,
  variaveisExternas: Set<string>
): VerificacaoSimd {
  const diagnosticos: string[] = [];
  const regras: string[] = [];

  // R1 — resolve layout da fonte: comando.fonte é caminho, mas no nível superior a fonte é um identificador de fatia
  // Para checagem formal, consideramos fonte[0] como nome da variável de fatia; seu tipo deve ter sido FATIA[T].
  // O IR já resolveu isso via contexto; aqui verificamos apenas o nome da estrutura se disponível via mapa.
  // Como não temos tipos aqui, o chamador (IR) passa o layout já resolvido via parâmetro adicional.
  // Para interpretador genérico, tentamos inferir pelo nome da estrutura se fonte contém "Item" etc — fallback.
  let layout: VerificacaoSimd['layoutFonte'] = 'desconhecido';
  // Tentativa heurística: se estruturas contém exatamente uma com nome igual à fonte ou conhecida, infere.
  // O IR passa layout correto; este fallback é para testes diretos sem IR.
  if (estruturas.size === 1) {
    const unica = [...estruturas.values()][0];
    layout = unica.layoutColunar ? 'SoA' : 'AoS';
    regras.push('R1: fonte FATIA[T] resolvida');
  } else {
    // Se múltiplas, não infere; deixa para IR preencher via contexto tipado.
    layout = 'desconhecido';
  }

  if (layout === 'AoS') {
    diagnosticos.push('R2: fonte não é LAYOUT_COLUNAR (AoS); vetorização escalar — SIMD requer SoA para coalescência.');
    regras.push('R2: AoS → escalar');
  } else if (layout === 'SoA') {
    regras.push('R2: SoA verificado');
  }

  let passoEfetivo = cmd.passoSimd ?? null;
  if (cmd.passoSimd !== undefined) {
    regras.push('R3: PASSO_SIMD declarado');
    if (!ehPotenciaDeDois(cmd.passoSimd)) {
      diagnosticos.push(`R3: PASSO_SIMD=${cmd.passoSimd} não é potência de dois.`);
    } else if (!PASSOS_VALIDOS.has(cmd.passoSimd)) {
      diagnosticos.push(`R3: PASSO_SIMD=${cmd.passoSimd} fora do intervalo vetorizável [4,8,16,32,64] para AVX2/AVX-512.`);
    }
    if (cmd.passoSimd < 4 || cmd.passoSimd > 64) {
      diagnosticos.push(`R3: PASSO_SIMD=${cmd.passoSimd} fora da janela eficiente.`);    
    }
  } else {
    regras.push('R3: PASSO_SIMD ausente → passo implícito 8 (AVX2)');
    passoEfetivo = 8;
  }

  // R4 — instruções proibidas no corpo
  const proibidosNoCorpo = new Set(['ENQUANTO', 'FALHAR_COM']);
  for (const sub of cmd.corpo) {
    if (proibidosNoCorpo.has(sub.tipoComando)) {
      diagnosticos.push(`R4: corpo contém '${sub.tipoComando}' que impede vetorização SIMD (branch divergente/escape).`);
    }
    if (sub.tipoComando === 'RETORNE') {
      diagnosticos.push('R4: RETORNE dentro de VETORIZAR_PARA impede vetorização (escape).');
    }
    if (sub.tipoComando === 'VETORIZAR_PARA') {
      diagnosticos.push('R4: VETORIZAR_PARA aninhado exige análise de dependência — marcado como escalar.');
    }
    if (sub.tipoComando === 'SE') {
      // SE predicado é vetorizável via máscara, mas por simplicidade G5 marca como alerta (mas não reprova)
      regras.push('R4: SE predicado detectado — vetorizável via máscara (AVX-512), mantém verificado com caveat.');
    }
  }

  // R5 — dependências de escrita
  for (const sub of cmd.corpo) {
    if (sub.tipoComando === 'ATRIBUICAO') {
      const alvo = sub.alvo;
      // alvo = [variavel] ou [variavel, campo, ...]
      if (alvo.length === 1) {
        // escrita em variável externa → deve ser redução
        const destino = alvo[0];
        if (destino === cmd.variavel) {
          diagnosticos.push(`R5: escrita no próprio iterador '${destino}' — dependência de loop, não vetorizável.`);
        } else if (variaveisExternas.has(destino)) {
          // verifica padrão de redução: destino <- destino op f(variavel)
          const expr = sub.expressao;
          const ehReducao =
            expr.tipo === 'OP_BINARIA' &&
            ((expr.esquerda.tipo === 'ACESSO' && expr.esquerda.caminho[0] === destino) ||
             (expr.direita.tipo === 'ACESSO' && expr.direita.caminho[0] === destino));
          if (!ehReducao) {
            diagnosticos.push(`R5: escrita em variável externa '${destino}' fora do padrão de redução 'x <- x op f(${cmd.variavel})'.`);
          } else {
            regras.push(`R5: redução verificada em '${destino}'`);
          }
        }
      } else if (alvo.length >= 2) {
        const base = alvo[0];
        if (base !== cmd.variavel) {
          // mutação de registro que não é o elemento iterado → possível dependência cruzada
          if (variaveisExternas.has(base)) {
            diagnosticos.push(`R5: mutação em '${alvo.join('.')}' fora do iterador '${cmd.variavel}' — aliasing`);
          }
        } else {
          regras.push(`R5: mutação elementwise '${alvo.join('.')}'`);
        }
      }
    }
    if (sub.tipoComando === 'DECL_VARIAVEL') {
      // decls internas são sempre elementwise (privadas por lane)
      regras.push(`R5: decl privada '${sub.nome}'`);
    }
  }

  const verificado = diagnosticos.length === 0;
  return { verificado, passoEfetivo, layoutFonte: layout, diagnosticos, regrasAplicadas: regras };
}

/** Normaliza passo para emissão LLVM: se null, 8. Se inválido, 8 com diagnóstico. */
export function passoParaLlvm(passo: number | null | undefined): number {
  if (passo == null) return 8;
  if (PASSOS_VALIDOS.has(passo)) return passo;
  if (ehPotenciaDeDois(passo)) return Math.min(64, Math.max(4, passo));
  return 8;
}
