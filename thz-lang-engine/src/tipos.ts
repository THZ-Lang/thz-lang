/* ============================================================
 * THZ-LANG — Sistema de Tipos Canônico (análise semântica)
 * ============================================================ */

export enum CategoriaTipo {
  PRIMITIVO = 'PRIMITIVO',
  INTEIRO = 'INTEIRO',
  DECIMAL = 'DECIMAL',
  MONETARIO = 'MONETARIO',
  FATIA = 'FATIA',
  REGISTRO = 'REGISTRO',
  ENUMERACAO = 'ENUMERACAO',
  RESULTADO = 'RESULTADO'
}

export interface TipoThz {
  /** Nome verbatim como escrito no fonte (ex.: 'DECIMAL(12,4)'). */
  nome: string;
  categoria: CategoriaTipo;
  /** Casas decimais para DECIMAL/MONETARIO. */
  escala?: number;
  /** Código ISO 4217 para MONETARIO. */
  moeda?: string;
  /** Tipo interno para FATIA[T] e canal de sucesso de RESULTADO[T,E]. */
  interno?: string;
  /** Canal de erro de RESULTADO[T,E]. */
  internoErro?: string;
}

/** Marcador especial para literais inteiros: cabem em inteiro ou decimal. */
export const TIPO_LITERAL_INTEIRO: TipoThz = { nome: '<literal-inteiro>', categoria: CategoriaTipo.PRIMITIVO };

const PRIMITIVOS: Readonly<Record<string, TipoThz>> = Object.freeze({
  TEXTO: { nome: 'TEXTO', categoria: CategoriaTipo.PRIMITIVO },
  LOGICO: { nome: 'LOGICO', categoria: CategoriaTipo.PRIMITIVO },
  UUID: { nome: 'UUID', categoria: CategoriaTipo.PRIMITIVO },
  DATA: { nome: 'DATA', categoria: CategoriaTipo.PRIMITIVO },
  DATA_HORA: { nome: 'DATA_HORA', categoria: CategoriaTipo.PRIMITIVO }
});

const INTEIROS: Readonly<Record<string, TipoThz>> = Object.freeze({
  NATURAL8: { nome: 'NATURAL8', categoria: CategoriaTipo.INTEIRO },
  NATURAL16: { nome: 'NATURAL16', categoria: CategoriaTipo.INTEIRO },
  NATURAL32: { nome: 'NATURAL32', categoria: CategoriaTipo.INTEIRO },
  NATURAL64: { nome: 'NATURAL64', categoria: CategoriaTipo.INTEIRO },
  INTEIRO8: { nome: 'INTEIRO8', categoria: CategoriaTipo.INTEIRO },
  INTEIRO16: { nome: 'INTEIRO16', categoria: CategoriaTipo.INTEIRO },
  INTEIRO32: { nome: 'INTEIRO32', categoria: CategoriaTipo.INTEIRO },
  INTEIRO64: { nome: 'INTEIRO64', categoria: CategoriaTipo.INTEIRO }
});

/** Registro completo de nomes primitivos reconhecidos pela análise. */
export const TIPOS_PRIMITIVOS: Readonly<Record<string, TipoThz>> = Object.freeze({
  ...PRIMITIVOS,
  ...INTEIROS
});

/**
 * Interpreta um nome de tipo verbatim do fonte.
 * Retorna undefined quando o nome não corresponde a nenhum tipo conhecido.
 */
export function analisarNomeTipo(nomeVerbatim: string): TipoThz | undefined {
  const primitivo = TIPOS_PRIMITIVOS[nomeVerbatim];
  if (primitivo) return primitivo;

  let casamento = /^DECIMAL\s*\(\s*\d+\s*,\s*(\d+)\s*\)$/.exec(nomeVerbatim);
  if (casamento) {
    return { nome: nomeVerbatim.replace(/\s+/g, ''), categoria: CategoriaTipo.DECIMAL, escala: Number.parseInt(casamento[1], 10) };
  }

  casamento = /^MONETARIO\s*\(\s*"?([A-Z]{3})"?\s*\)$/.exec(nomeVerbatim);
  if (casamento) {
    return { nome: nomeVerbatim.replace(/\s+/g, ''), categoria: CategoriaTipo.MONETARIO, moeda: casamento[1] };
  }

  casamento = /^RESULTADO\s*\[\s*([^,\]]+?)\s*,\s*([^,\]]+?)\s*\]$/.exec(nomeVerbatim);
  if (casamento) {
    return {
      nome: nomeVerbatim.replace(/\s+/g, ''),
      categoria: CategoriaTipo.RESULTADO,
      interno: casamento[1].trim(),
      internoErro: casamento[2].trim()
    };
  }

  casamento = /^FATIA\s*\[\s*(\w+)\s*\]$/.exec(nomeVerbatim);
  if (casamento) {
    return { nome: nomeVerbatim.replace(/\s+/g, ''), categoria: CategoriaTipo.FATIA, interno: casamento[1] };
  }

  // Estruturas declaradas no programa são resolvidas pelo analisador.
  return undefined;
}

export function ehInteiro(t?: TipoThz): boolean {
  return t?.categoria === CategoriaTipo.INTEIRO || t === TIPO_LITERAL_INTEIRO;
}

export function ehNumerico(t?: TipoThz): boolean {
  return ehInteiro(t) || t?.categoria === CategoriaTipo.DECIMAL;
}

/**
 * Compatibilidade de atribuição/comparação.
 * Regras v2.2:
 *  - nomes idênticos são sempre compatíveis;
 *  - inteiros (e literais inteiros) convertem implicitamente para DECIMAL;
 *  - MONETARIO nunca mistura com numéricos nem com outras moedas;
 *  - FATIA/REGISTRO exigem identidade nominal.
 * Tipos desconhecidos (undefined) retornam true para não cascatear erros já reportados.
 */
export function saoCompativeis(origem?: TipoThz, destino?: TipoThz): boolean {
  if (!origem || !destino) return true;
  if (origem.nome === destino.nome) return true;
  if (ehInteiro(origem) && ehInteiro(destino)) return true;
  if (ehNumerico(origem) && destino.categoria === CategoriaTipo.DECIMAL) return true;

  return false;
}

/** Descrição legível para mensagens de erro. */
export function descrever(t?: TipoThz): string {
  return t ? "'" + t.nome + "'" : '<desconhecido>';
}
