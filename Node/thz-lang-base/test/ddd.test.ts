import { test } from 'node:test';
import assert from 'node:assert/strict';
import { ThzLexer } from '../src/lexer.js';
import { ThzParser } from '../src/parser.js';
import { AnalisadorSemantico } from '../src/analisador.js';
import { InterpretadorThz, ValorThz, ENUMERADO, DECIMAL } from '../src/interpretador.js';
import { DecimalFixo } from '../src/runtime.js';

function parsear(fonte: string) {
  const tokens = new ThzLexer(fonte).tokenize();
  return new ThzParser(tokens).parse();
}

function analisar(fonte: string, estrito = false) {
  return new AnalisadorSemantico(parsear(fonte)).analisar({ estrito });
}

const PROGRAMA_PEDIDOS = `
VERSAO_LINGUAGEM "2.2"
PROGRAMA GestaoPedidos
METADADOS_ARQUITETURA
    DOMINIO: "VendasEFaturamento"
    SUBDOMINIO: "ClassificacaoPedidos"
    CAMADA: "Dominio"
    VERSAO: "2.2.0"
    AUTOR: "Lucas Thomaz"
    SLO_LATENCIA_MAXIMA: "15ms"
    CONFORMIDADE: "SOX-404"
FIM_METADADOS
ENUMERACAO StatusPedido
    PENDENTE
    APROVADO
    REJEITADO
FIM_ENUMERACAO
ESTRUTURA Pedido
    codigo        : TEXTO
    valor_total   : DECIMAL(14, 2)
    status        : StatusPedido
    INVARIANTE valor_total >= 0.00
FIM_ESTRUTURA
REGRA_NEGOCIO ClassificacaoPedidos
    IDENTIFICADOR_REGRA: "BR-VENDAS-2026-01"
    RASTREIO_REQUISITO: "REQ-VENDAS-3301"
    CONTRATO_ENTRADA
        EXIGE pedido.valor_total >= 0.00
    FIM_CONTRATO_ENTRADA
    CONTRATO_SAIDA
        GARANTE pedido.status <> REJEITADO
    FIM_CONTRATO_SAIDA
    OPERACAO Classificar(pedido: FATIA[Pedido]) : RESULTADO[StatusPedido, TEXTO]
    INICIO
        VETORIZAR_PARA p EM pedido PASSO_SIMD 8
            SE p.valor_total > 100.00
                p.status <- APROVADO
                RETORNE APROVADO
            SENAO
                FALHAR_COM "Valor abaixo do minimo"
            FIM_SE
        FIM_PARA
        RETORNE PENDENTE
    FIM
FIM_REGRA_NEGOCIO
FIM_PROGRAMA
`;

function registroPedido(valorTotal: string): ValorThz {
  const campos = new Map<string, ValorThz>();
  campos.set('codigo', { classe: 'TEXTO', valor: 'PED-001' });
  campos.set('valor_total', DECIMAL(DecimalFixo.deTexto(valorTotal, 2)));
  campos.set('status', ENUMERADO('StatusPedido', 'PENDENTE'));
  return { classe: 'REGISTRO', nomeEstrutura: 'Pedido', campos };
}

/* ---------------- Parser ---------------- */

test('Parser — ENUMERACAO captura membros na AST', () => {
  const ast = parsear(PROGRAMA_PEDIDOS);
  assert.equal(ast.enumeracoes.length, 1);
  assert.deepEqual(ast.enumeracoes[0].membros, ['PENDENTE', 'APROVADO', 'REJEITADO']);
});

test('Parser — INVARIANTE captura expressão e texto canônico', () => {
  const ast = parsear(PROGRAMA_PEDIDOS);
  const invariantes = ast.estruturas[0].invariantes;
  assert.equal(invariantes.length, 1);
  assert.match(invariantes[0].textoCanonico, /valor_total >= 0\.00/);
});

test('Parser — tipo RESULTADO[T,E] preservado verbatim', () => {
  const ast = parsear(PROGRAMA_PEDIDOS);
  assert.equal(ast.regras[0].operacoes[0].tipoRetorno, 'RESULTADO[StatusPedido, TEXTO]');
});

/* ---------------- Análise semântica ---------------- */

test('Analisador — programa DDD completo é aprovado', () => {
  const erros = analisar(PROGRAMA_PEDIDOS, true);
  assert.equal(erros.length, 0, JSON.stringify(erros, null, 2));
});

test('Analisador — membro de enumeração não declarado como variável resolve', () => {
  const erros = analisar(PROGRAMA_PEDIDOS.replace('RETORNE PENDENTE', 'VARIAVEL s : StatusPedido <- PENDENTE\nRETORNE s'));
  assert.equal(erros.length, 0, JSON.stringify(erros));
});

test('Analisador — RETORNE deve alimentar o canal de sucesso T', () => {
  const erros = analisar(PROGRAMA_PEDIDOS.replace('RETORNE APROVADO', 'RETORNE "aprovado"'));
  assert.ok(erros.some((e) => /RETORNE incompatível/.test(e.mensagem)), JSON.stringify(erros));
});

test('Analisador — FALHAR_COM exige retorno RESULTADO[T,E]', () => {
  const fonte = PROGRAMA_PEDIDOS.replace(
    ') : RESULTADO[StatusPedido, TEXTO]',
    ') : DECIMAL(14, 2)'
  );
  const erros = analisar(fonte);
  assert.ok(erros.some((e) => /FALHAR_COM exige operação com retorno/.test(e.mensagem)), JSON.stringify(erros));
});

test('Analisador — canal de erro E verificado no FALHAR_COM', () => {
  const fonte = PROGRAMA_PEDIDOS.replace('FALHAR_COM "Valor abaixo do minimo"', 'FALHAR_COM 42');
  const erros = analisar(fonte);
  assert.ok(erros.some((e) => /FALHAR_COM incompatível com o canal de erro/.test(e.mensagem)), JSON.stringify(erros));
});

/* ---------------- Interpretador ---------------- */

test('Interpretador — caminho de sucesso embrulha em RESULTADO(sucesso)', () => {
  const interpretador = new InterpretadorThz(parsear(PROGRAMA_PEDIDOS));
  const resultado = interpretador.executarOperacao('Classificar', {
    pedido: { classe: 'FATIA', tipoInterno: 'Pedido', elementos: [registroPedido('250.00')] }
  });
  assert.equal(resultado?.classe, 'RESULTADO');
  if (resultado?.classe === 'RESULTADO') {
    assert.equal(resultado.sucesso, true);
    assert.deepEqual(resultado.valor, ENUMERADO('StatusPedido', 'APROVADO'));
  }
});

test('Interpretador — FALHAR_COM devolve RESULTADO(falha) e pula GARANTE', () => {
  // GARANTE exigiria status <> REJEITADO; a falha ignora o contrato de saída.
  const interpretador = new InterpretadorThz(parsear(PROGRAMA_PEDIDOS));
  const resultado = interpretador.executarOperacao('Classificar', {
    pedido: { classe: 'FATIA', tipoInterno: 'Pedido', elementos: [registroPedido('50.00')] }
  });
  assert.equal(resultado?.classe, 'RESULTADO');
  if (resultado?.classe === 'RESULTADO') {
    assert.equal(resultado.sucesso, false);
    assert.deepEqual(resultado.erro, { classe: 'TEXTO', valor: 'Valor abaixo do minimo' });
  }
});

test('Interpretador — FALHAR_COM sem retorno RESULTADO é erro explícito', () => {
  const ast = parsear(PROGRAMA_PEDIDOS);
  ast.regras[0].operacoes[0].tipoRetorno = 'DECIMAL(14, 2)';
  ast.regras[0].operacoes[0].corpo = [
    { tipoComando: 'DECL_VARIAVEL', nome: 'motivo', tipoDado: 'TEXTO',
      inicializacao: { tipo: 'LITERAL_TEXTO', valor: 'erro', linha: 1, coluna: 1 }, linha: 1, coluna: 1 },
    { tipoComando: 'FALHAR_COM', expressao: { tipo: 'ACESSO', caminho: ['motivo'], linha: 2, coluna: 1 }, linha: 2, coluna: 1 }
  ];
  const interpretador = new InterpretadorThz(ast);
  assert.throws(
    () => interpretador.executarOperacao('Classificar', {
      pedido: { classe: 'FATIA', tipoInterno: 'Pedido', elementos: [registroPedido('50.00')] }
    }),
    /FALHAR_COM exige operação com retorno RESULTADO/
  );
});

test('Interpretador — igualdade de ENUMERADO em GARANTE reprova quando falsa', () => {
  const fonte = PROGRAMA_PEDIDOS.replace('GARANTE pedido.status <> REJEITADO', 'GARANTE pedido.status = REJEITADO');
  const interpretador = new InterpretadorThz(parsear(fonte));
  assert.throws(
    () => interpretador.executarOperacao('Classificar', {
      pedido: { classe: 'FATIA', tipoInterno: 'Pedido', elementos: [registroPedido('250.00')] }
    }),
    /Violação de Contrato GARANTE/
  );
});

test('Interpretador — INVARIANTE reprova registro fora do domínio', () => {
  const interpretador = new InterpretadorThz(parsear(PROGRAMA_PEDIDOS));
  assert.throws(() => interpretador.validarInvariantes(registroPedido('-1.00')), /Violação de Invariante/);
  // Dentro do domínio: nenhuma exceção.
  interpretador.validarInvariantes(registroPedido('99.90'));
});

test('Interpretador — EXIBA imprime membro da enumeração', () => {
  const fonte = `
PROGRAMA GestaoPedidos
ENUMERACAO StatusPedido
    PENDENTE
    APROVADO
FIM_ENUMERACAO
REGRA_NEGOCIO R
    OPERACAO Op() : DECIMAL(10, 2)
    INICIO
        EXIBA "STATUS=" + APROVADO
        RETORNE 0.0
    FIM
FIM_REGRA_NEGOCIO
FIM_PROGRAMA
`;
  const linhas: string[] = [];
  const interpretador = new InterpretadorThz(parsear(fonte), { saida: (l) => linhas.push(l) });
  interpretador.executarOperacao('Op', {});
  assert.deepEqual(linhas, ['STATUS=APROVADO']);
});
