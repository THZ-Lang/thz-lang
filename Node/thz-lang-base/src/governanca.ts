/* ============================================================
 * THZ-LANG — Governança Auditável (G4)
 * Matriz RASTREIO_REQUISITO → Regra → Contrato → Teste golden
 * ============================================================ */

import type { ProgramaAST } from './types.js';

export interface LinhaGovernanca {
  regra: string;
  identificador?: string;
  rastreio?: string;
  descricao?: string;
  exige: number;
  garante: number;
  invariantesVinculadas: number;
  operacoes: number;
  status: 'ok' | 'alerta' | 'reprovado';
  violacoes: string[];
}

export interface AuditoriaGovernanca {
  programa: string;
  versaoLinguagem?: string;
  metadados?: {
    dominio: string;
    subdominio: string;
    camada: string;
    versao: string;
    sloLatencia: string;
    conformidade: string[];
  };
  regras: LinhaGovernanca[];
  estruturas: { nome: string; layoutColunar: boolean; invariantes: number; campos: number }[];
  enumeracoes: { nome: string; membros: number }[];
  metricas: {
    totalRegras: number;
    comRastreio: number;
    comIdentificador: number;
    comContrato: number;
    comDescricao: number;
    totalExige: number;
    totalGarante: number;
    totalInvariantes: number;
    coberturaRastreio: number;
    coberturaContrato: number;
  };
  pendencias: string[];
  aprovada: boolean;
}

export interface OpcoesAuditoria {
  /** Quando true, ausências de rastreio/contrato/SLO reprovam a auditoria (igual ao lint --estrito). */
  estrito?: boolean;
}

export function auditar(ast: ProgramaAST, opcoes: OpcoesAuditoria = {}): AuditoriaGovernanca {
  const pendencias: string[] = [];
  const rastreiosVistos = new Map<string, string>(); // rastreio -> regra nome

  if (opcoes.estrito && !ast.versaoLinguagem) pendencias.push('Pragma VERSAO_LINGUAGEM ausente.');
  if (!ast.metadados?.sloLatencia) {
    const msg = 'METADADOS_ARQUITETURA sem SLO_LATENCIA_MAXIMA.';
    if (opcoes.estrito) pendencias.push(msg);
  }

  const regras: LinhaGovernanca[] = ast.regras.map((r) => {
    const violacoes: string[] = [];
    const exige = r.clausulasEntrada.length;
    const garante = r.clausulasSaida.length;
    const operacoes = r.operacoes.length;

    if (!r.identificador) violacoes.push('IDENTIFICADOR_REGRA ausente.');
    if (!r.rastreioRequisito) violacoes.push('RASTREIO_REQUISITO ausente.');
    else {
      const dono = rastreiosVistos.get(r.rastreioRequisito);
      if (dono) violacoes.push(`RASTREIO_REQUISITO duplicado: '${r.rastreioRequisito}' já usado em '${dono}'.`);
      else rastreiosVistos.set(r.rastreioRequisito, r.nome);
    }
    if (!r.descricao) violacoes.push('DESCRICAO ausente.');
    if (exige === 0 && garante === 0) violacoes.push('Regra sem contratos formais (EXIGE/GARANTE).');

    // invariantes vinculadas: estruturas referenciadas por FATIA[Struct] nos parâmetros
    const estruturasReferenciadas = new Set<string>();
    for (const op of r.operacoes) {
      for (const p of op.parametros) {
        const m = /^FATIA\[(\w+)\]$/.exec(p.tipo);
        if (m) estruturasReferenciadas.add(m[1]);
      }
    }
    let invariantesVinculadas = 0;
    for (const nome of estruturasReferenciadas) {
      const est = ast.estruturas.find((e) => e.nome === nome);
      if (est) invariantesVinculadas += est.invariantes.length;
    }

    const status: LinhaGovernanca['status'] = violacoes.length === 0 ? 'ok' : opcoes.estrito ? 'reprovado' : violacoes.some((v) => v.includes('duplicado') || v.includes('sem contratos')) ? 'reprovado' : 'alerta';

    return {
      regra: r.nome,
      identificador: r.identificador,
      rastreio: r.rastreioRequisito,
      descricao: r.descricao,
      exige,
      garante,
      invariantesVinculadas,
      operacoes,
      status,
      violacoes,
    };
  });

  // pendências globais adicionais (fora de regras)
  for (const r of regras) {
    if (opcoes.estrito) {
      for (const v of r.violacoes) pendencias.push(`[${r.regra}] ${v}`);
    }
  }

  const totalRegras = regras.length;
  const comRastreio = regras.filter((r) => !!r.rastreio).length;
  const comIdentificador = regras.filter((r) => !!r.identificador).length;
  const comContrato = regras.filter((r) => r.exige + r.garante > 0).length;
  const comDescricao = regras.filter((r) => !!r.descricao).length;
  const totalExige = regras.reduce((a, r) => a + r.exige, 0);
  const totalGarante = regras.reduce((a, r) => a + r.garante, 0);
  const totalInvariantes = ast.estruturas.reduce((a, e) => a + e.invariantes.length, 0);

  const coberturaRastreio = totalRegras === 0 ? 1 : comRastreio / totalRegras;
  const coberturaContrato = totalRegras === 0 ? 1 : comContrato / totalRegras;

  const aprovada = opcoes.estrito ? pendencias.length === 0 : coberturaRastreio >= 0.5 && coberturaContrato >= 0.5;

  return {
    programa: ast.nome,
    versaoLinguagem: ast.versaoLinguagem,
    metadados: ast.metadados
      ? {
          dominio: ast.metadados.dominio,
          subdominio: ast.metadados.subdominio,
          camada: ast.metadados.camada,
          versao: ast.metadados.versao,
          sloLatencia: ast.metadados.sloLatencia,
          conformidade: [...ast.metadados.conformidade],
        }
      : undefined,
    regras,
    estruturas: ast.estruturas.map((e) => ({ nome: e.nome, layoutColunar: e.layoutColunar, invariantes: e.invariantes.length, campos: e.campos.length })),
    enumeracoes: ast.enumeracoes.map((e) => ({ nome: e.nome, membros: e.membros.length })),
    metricas: {
      totalRegras,
      comRastreio,
      comIdentificador,
      comContrato,
      comDescricao,
      totalExige,
      totalGarante,
      totalInvariantes,
      coberturaRastreio,
      coberturaContrato,
    },
    pendencias,
    aprovada,
  };
}

export function gerarMarkdownGovernanca(a: AuditoriaGovernanca): string {
  const pct = (v: number) => `${(v * 100).toFixed(0)}%`;
  let md = `# Relatório de Governança — ${a.programa}\n\n`;
  md += `> **Versão da linguagem:** ${a.versaoLinguagem ? `\`VERSAO_LINGUAGEM "${a.versaoLinguagem}"\`` : '*(ausente — assumindo corrente)*'}  \n`;
  if (a.metadados) {
    md += `> **Domínio:** ${a.metadados.dominio} / ${a.metadados.subdominio} — Camada \`${a.metadados.camada}\` — SLO \`${a.metadados.sloLatencia}\` — Conformidade: ${a.metadados.conformidade.join(', ') || '—'}  \n`;
  }
  md += `> **Status:** ${a.aprovada ? '✅ Aprovada' : '❌ Reprovada (pendências)'}  \n\n`;

  md += `## 1. Métricas\n\n`;
  md += `| Métrica | Valor |\n|---|---|\n`;
  md += `| Regras totais | ${a.metricas.totalRegras} |\n`;
  md += `| Com RASTREIO_REQUISITO | ${a.metricas.comRastreio} / ${a.metricas.totalRegras} (${pct(a.metricas.coberturaRastreio)}) |\n`;
  md += `| Com IDENTIFICADOR_REGRA | ${a.metricas.comIdentificador} / ${a.metricas.totalRegras} |\n`;
  md += `| Com contrato (EXIGE/GARANTE) | ${a.metricas.comContrato} / ${a.metricas.totalRegras} (${pct(a.metricas.coberturaContrato)}) |\n`;
  md += `| Com DESCRICAO | ${a.metricas.comDescricao} / ${a.metricas.totalRegras} |\n`;
  md += `| Total EXIGE | ${a.metricas.totalExige} |\n`;
  md += `| Total GARANTE | ${a.metricas.totalGarante} |\n`;
  md += `| Total INVARIANTE (estruturas) | ${a.metricas.totalInvariantes} |\n\n`;

  md += `## 2. Matriz de Rastreabilidade\n\n`;
  md += `| Regra | RASTREIO_REQUISITO | IDENTIFICADOR_REGRA | EXIGE | GARANTE | Invariantes vinculadas | Status |\n|---|---|---|---|---|---|---|\n`;
  for (const r of a.regras) {
    const statusIcon = r.status === 'ok' ? '✅' : r.status === 'alerta' ? '⚠️' : '❌';
    md += `| \`${r.regra}\` | \`${r.rastreio ?? '—'}\` | \`${r.identificador ?? '—'}\` | ${r.exige} | ${r.garante} | ${r.invariantesVinculadas} | ${statusIcon} ${r.status} |\n`;
  }
  if (a.regras.length === 0) md += `| *(nenhuma regra)* | — | — | — | — | — | — |\n`;
  md += `\n`;

  md += `## 3. Estruturas & Enumerações\n\n`;
  md += `**Estruturas:** ${a.estruturas.length === 0 ? '—' : a.estruturas.map((e) => `\`${e.nome}\` (${e.campos} campos${e.layoutColunar ? ', LAYOUT_COLUNAR' : ''}${e.invariantes ? `, ${e.invariantes} invariante(s)` : ''})`).join(', ')}  \n\n`;
  md += `**Enumerações:** ${a.enumeracoes.length === 0 ? '—' : a.enumeracoes.map((e) => `\`${e.nome}\` (${e.membros} membros)`).join(', ')}  \n\n`;

  md += `## 4. Pendências\n\n`;
  if (a.pendencias.length === 0) md += `Nenhuma pendência — auditoria limpa.\n`;
  else {
    for (const p of a.pendencias) md += `- ❌ ${p}\n`;
  }
  md += `\n`;

  md += `## 5. Mermaid — Cadeia de Governo\n\n`;
  md += '```mermaid\ngraph TD\n';
  for (const r of a.regras) {
    const req = r.rastreio ?? 'REQ-?';
    const id = r.identificador ?? r.regra;
    md += `  ${req}["${req}"] --> ${r.regra}["${r.regra}<br/>${id}"]\n`;
    if (r.exige > 0) md += `  ${r.regra} --> EXIGE_${r.regra}["EXIGE x${r.exige}"]\n`;
    if (r.garante > 0) md += `  ${r.regra} --> GARANTE_${r.regra}["GARANTE x${r.garante}"]\n`;
    if (r.invariantesVinculadas > 0) md += `  ${r.regra} -.-> INV_${r.regra}["INVARIANTE x${r.invariantesVinculadas}"]\n`;
  }
  md += '```\n';
  return md;
}
