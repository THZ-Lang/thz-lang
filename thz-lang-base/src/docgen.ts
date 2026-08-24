import { ProgramaAST } from './types.js';

export class ThzDocGen {
  public static gerarMarkdown(ast: ProgramaAST): string {
    const crase3 = String.fromCharCode(96, 96, 96);
    const crase1 = String.fromCharCode(96);

    let doc = '# Especificação Arquitetural e Dicionário de Domínio: ' + ast.nome + '\n\n';

    if (ast.metadados) {
      doc += '## 1. Metadados de Governança (ISO/IEC/IEEE 42010)\n\n';
      doc += '| Atributo | Valor |\n| :--- | :--- |\n';
      doc += '| **Domínio** | ' + ast.metadados.dominio + ' |\n';
      doc += '| **Subdomínio** | ' + ast.metadados.subdominio + ' |\n';
      doc += '| **Camada** | ' + ast.metadados.camada + ' |\n';
      doc += '| **Versão** | ' + ast.metadados.versao + ' |\n';
      doc += '| **Autor** | ' + ast.metadados.autor + ' |\n';
      doc += '| **SLO Latência** | ' + ast.metadados.sloLatencia + ' |\n';
      doc += '| **Conformidade** | ' + ast.metadados.conformidade.join(', ') + ' |\n\n';
    }

    if (ast.versaoLinguagem) {
      doc += '> **Compatibilidade:** programa declara `VERSAO_LINGUAGEM "' + ast.versaoLinguagem + '"`.\n\n';
    }

    doc += '## 2. Estruturas de Dados e Layout Colunar\n\n';
    for (const est of ast.estruturas) {
      doc += '### Estrutura: ' + crase1 + est.nome + crase1 + ' ' + (est.layoutColunar ? '*(Layout Colunar / SIMD)*' : '') + '\n\n';
      doc += '| Campo | Tipo |\n| :--- | :--- |\n';
      for (const c of est.campos) {
        doc += '| ' + crase1 + c.nome + crase1 + ' | ' + crase1 + c.tipo + crase1 + ' |\n';
      }
      if (est.invariantes.length > 0) {
        doc += '\n**Invariantes (`INVARIANTE`):**\n\n';
        for (const inv of est.invariantes) {
          doc += '- ' + crase1 + inv.textoCanonico + crase1 + '\n';
        }
      }
      doc += '\n';
    }

    if (ast.enumeracoes.length > 0) {
      doc += '## 3. Enumerações de Domínio (`ENUMERACAO`)\n\n';
      for (const en of ast.enumeracoes) {
        doc += '- **' + en.nome + '**: ' + en.membros.map((m) => crase1 + m + crase1).join(', ') + '\n';
      }
      doc += '\n';
    }

    doc += '## 4. Regras de Negócio e Contratos Formais\n\n';
    for (const r of ast.regras) {
      doc += '### Regra: ' + crase1 + r.nome + crase1 + (r.identificador ? ' (ID: ' + crase1 + r.identificador + crase1 + ')' : '') + '\n\n';
      if (r.rastreioRequisito) doc += '- **Rastreio:** ' + crase1 + r.rastreioRequisito + crase1 + '\n';
      if (r.descricao) doc += '- **Descrição:** ' + r.descricao + '\n';

      if (r.clausulasEntrada.length > 0) {
        doc += '\n**Pré-condições (`EXIGE`):**\n\n';
        for (const c of r.clausulasEntrada) doc += '- ' + crase1 + c.textoCanonico + crase1 + '\n';
      }
      if (r.clausulasSaida.length > 0) {
        doc += '\n**Pós-condições (`GARANTE`):**\n\n';
        for (const c of r.clausulasSaida) doc += '- ' + crase1 + c.textoCanonico + crase1 + '\n';
      }

      for (const op of r.operacoes) {
        const assinatura = op.parametros.map((p) => p.nome + ': ' + p.tipo).join(', ');
        doc += '\n**Operação:** ' + crase1 + op.nome + '(' + assinatura + ') : ' + op.tipoRetorno + crase1;
        doc += op.corpo.length > 0 ? ' — corpo executável com ' + op.corpo.length + ' comando(s)\n' : ' — apenas assinatura declarada\n';
      }
      doc += '\n';
    }

    if (ast.procedimentos && ast.procedimentos.length > 0) {
      doc += '## 5. Procedimentos Gerais (`PROCEDIMENTO`)\n\n';
      for (const proc of ast.procedimentos) {
        const assinatura = proc.parametros.map((p) => p.nome + ': ' + p.tipo).join(', ');
        doc += '- **' + proc.nome + '(' + assinatura + ')** — ' + (proc.corpo.length > 0 ? proc.corpo.length + ' comando(s)' : 'sem corpo') + '\n';
      }
      doc += '\n';
    }

    doc += '## 5. Diagrama de Fluxo e Arquitetura Viva\n\n';
    doc += crase3 + 'mermaid\ngraph TD\n';
    const dom = (ast.metadados && ast.metadados.dominio) ? ast.metadados.dominio : 'Dominio';
    const sub = (ast.metadados && ast.metadados.subdominio) ? ast.metadados.subdominio : 'Subdominio';
    doc += '    subgraph BoundedContext [' + dom + ' / ' + sub + ']\n';
    for (const r of ast.regras) {
      for (const op of r.operacoes) {
        doc += '        Regra_' + r.nome + '["Regra: ' + r.nome + '<br/>ID: ' + (r.identificador ?? '—') + '"] --> Op_' + op.nome + '["Operação: ' + op.nome + '()"]\n';
      }
    }
    doc += '    end\n' + crase3 + '\n';

    return doc;
  }
}
