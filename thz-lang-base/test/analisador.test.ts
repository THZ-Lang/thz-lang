import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'fs';
import path from 'path';
import { ThzLexer } from '../src/lexer.js';
import { ThzParser } from '../src/parser.js';
import { AnalisadorSemantico, ErroSemantico } from '../src/analisador.js';

function analisarFonte(fonte: string, estrito: boolean = false): ErroSemantico[] {
  const tokens = new ThzLexer(fonte).tokenize();
  const ast = new ThzParser(tokens).parse();
  return new AnalisadorSemantico(ast).analisar({ estrito });
}

const PROGRAMA_VALIDO = `
PROGRAMA Teste
ESTRUTURA Ponto
    x : DECIMAL(10, 2)
    rotulo : TEXTO
FIM_ESTRUTURA
REGRA_NEGOCIO R
    OPERACAO Op(p: FATIA[Ponto]) : DECIMAL(18, 4)
    INICIO
        RETORNE 0.0
    FIM
FIM_REGRA_NEGOCIO
FIM_PROGRAMA
`;

test('Analisador — programa válido não produz erros', () => {
  const erros = analisarFonte(PROGRAMA_VALIDO);
  assert.equal(erros.length, 0, 'Esperado zero erros: ' + JSON.stringify(erros, null, 2));
});

test('Analisador — exemplo canônico aprovado no lint estrito', () => {
  const fonte = fs.readFileSync(path.join(process.cwd(), 'exemplos', 'faturamento.thz'), 'utf8');
  const erros = analisarFonte(fonte, true);
  assert.equal(erros.length, 0, 'Exemplo canônico deve passar no lint estrito: ' + JSON.stringify(erros, null, 2));
});

test('Analisador — identificador não declarado reportado com posição', () => {
  const fonte = PROGRAMA_VALIDO.replace(
    '        RETORNE 0.0',
    '        VARIAVEL a : DECIMAL(10, 2) <- b + 1\n        RETORNE a'
  );
  const erros = analisarFonte(fonte);
  assert.equal(erros.length, 1);
  assert.match(erros[0].mensagem, /Identificador não declarado: 'b'/);
  assert.equal(erros[0].linha, 10);
});

test('Analisador — campo inexistente na estrutura', () => {
  const fonte = PROGRAMA_VALIDO.replace(
    '        RETORNE 0.0',
    '        VETORIZAR_PARA item EM p\n            RETORNE item.campo_fantasma\n        FIM_PARA\n        RETORNE 0.0'
  );
  const erros = analisarFonte(fonte);
  assert.ok(erros.some((e) => /Campo 'campo_fantasma' inexistente na estrutura 'Ponto'/.test(e.mensagem)));
});

test('Analisador — atribuição de tipo incompatível', () => {
  const fonte = PROGRAMA_VALIDO.replace(
    '        RETORNE 0.0',
    '        VARIAVEL t : TEXTO <- "abc"\n        t <- 1.5\n        RETORNE 0.0'
  );
  const erros = analisarFonte(fonte);
  assert.ok(erros.some((e) => /Atribuição incompatível/.test(e.mensagem)));
});

test('Analisador — literal inteiro é compatível com DECIMAL declarada', () => {
  const fonte = PROGRAMA_VALIDO.replace(
    '        RETORNE 0.0',
    '        VARIAVEL d : DECIMAL(10, 2) <- 5\n        RETORNE d'
  );
  const erros = analisarFonte(fonte);
  assert.equal(erros.length, 0, 'Coerção inteiro → DECIMAL deve ser implícita: ' + JSON.stringify(erros));
});

test('Analisador — condição do SE deve ser lógica', () => {
  const fonte = PROGRAMA_VALIDO.replace(
    '        RETORNE 0.0',
    '        SE p.x + 1\n            RETORNE 1.0\n        FIM_SE\n        RETORNE 0.0'
  );
  const erros = analisarFonte(fonte);
  assert.ok(erros.some((e) => /Esperado valor lógico em condição do 'SE'/.test(e.mensagem)));
});

test('Analisador — fonte do VETORIZAR_PARA deve ser fatia', () => {
  const fonte = PROGRAMA_VALIDO.replace(
    '        RETORNE 0.0',
    '        VARIAVEL escalar : DECIMAL(10, 2) <- 0.00\n        VETORIZAR_PARA item EM escalar\n            EXIBA item\n        FIM_PARA\n        RETORNE 0.0'
  );
  const erros = analisarFonte(fonte);
  assert.ok(erros.some((e) => /deve ser FATIA\[T\]/.test(e.mensagem)));
});

test('Analisador — cláusula EXIGE deve ser expressão lógica', () => {
  const fonte = PROGRAMA_VALIDO.replace(
    'REGRA_NEGOCIO R',
    'REGRA_NEGOCIO R\nCONTRATO_ENTRADA\nEXIGE p.x + 1\nFIM_CONTRATO_ENTRADA'
  );
  const erros = analisarFonte(fonte);
  assert.ok(erros.some((e) => /Cláusula 'EXIGE' deve ser lógica/.test(e.mensagem)), JSON.stringify(erros));
});

test('Analisador — cláusula dentro da operação validada contra parâmetros', () => {
  const fonte = `
PROGRAMA Teste
ESTRUTURA Ponto
    x : DECIMAL(10, 2)
FIM_ESTRUTURA
REGRA_NEGOCIO R
    CONTRATO_ENTRADA
        EXIGE p.x >= 0.00
    FIM_CONTRATO_ENTRADA
    CONTRATO_SAIDA
        GARANTE p.y > 0.00
    FIM_CONTRATO_SAIDA
    OPERACAO Op(p: FATIA[Ponto]) : DECIMAL(18, 4)
    INICIO
        RETORNE 0.0
    FIM
FIM_REGRA_NEGOCIO
FIM_PROGRAMA
`;
  const erros = analisarFonte(fonte);
  assert.ok(erros.some((e) => /Campo 'y' inexistente na estrutura 'Ponto'/.test(e.mensagem)), JSON.stringify(erros));
});

test('Analisador — RETORNE incompatível com tipo declarado', () => {
  const fonte = PROGRAMA_VALIDO.replace('        RETORNE 0.0', '        RETORNE "fim"');
  const erros = analisarFonte(fonte);
  assert.ok(erros.some((e) => /RETORNE incompatível/.test(e.mensagem)));
});

test('Analisador — divisão por constante zero detectada (inteiro e decimal)', () => {
  const fonte = PROGRAMA_VALIDO.replace(
    '        RETORNE 0.0',
    '        VARIAVEL z : DECIMAL(18, 4) <- 1 / 0\n        RETORNE z'
  );
  const erros = analisarFonte(fonte);
  assert.ok(erros.some((e) => /Divisão por constante zero/.test(e.mensagem)), JSON.stringify(erros));

  const fonteDecimal = PROGRAMA_VALIDO.replace(
    '        RETORNE 0.0',
    '        VARIAVEL z : DECIMAL(18, 4) <- 10.0 / 0.0\n        RETORNE z'
  );
  const errosDecimal = analisarFonte(fonteDecimal);
  assert.ok(errosDecimal.some((e) => /Divisão por constante zero/.test(e.mensagem)));
});

test('Analisador — redeclaração no mesmo escopo', () => {
  const fonte = PROGRAMA_VALIDO.replace(
    '        RETORNE 0.0',
    '        VARIAVEL a : DECIMAL(4, 2) <- 1.00\n        VARIAVEL a : DECIMAL(4, 2) <- 2.00\n        RETORNE a'
  );
  const erros = analisarFonte(fonte);
  assert.ok(erros.some((e) => /Redeclaração de 'a'/.test(e.mensagem)));
});

test('Lint estrito — pragma ausente', () => {
  const semPragma = PROGRAMA_VALIDO.trimStart();
  assert.match(PROGRAMA_VALIDO, /^\s*PROGRAMA/, 'Template de teste não deve conter pragma.');
  const erros = analisarFonte(semPragma, true);
  assert.ok(erros.some((e) => /Pragma VERSAO_LINGUAGEM ausente/.test(e.mensagem)), JSON.stringify(erros));
});

test('Lint estrito — METADADOS sem SLO_LATENCIA_MAXIMA', () => {
  const fonte = PROGRAMA_VALIDO.replace(
    'PROGRAMA Teste',
    'VERSAO_LINGUAGEM "2.2"\nPROGRAMA Teste\nMETADADOS_ARQUITETURA\nDOMINIO: "Teste"\nFIM_METADADOS'
  );
  const erros = analisarFonte(fonte, true);
  assert.ok(erros.some((e) => /SLO_LATENCIA_MAXIMA/.test(e.mensagem)), JSON.stringify(erros));
});

test('Lint estrito — regra exige IDENTIFICADOR_REGRA e RASTREIO_REQUISITO', () => {
  const fonte = PROGRAMA_VALIDO.replace(
    'PROGRAMA Teste',
    'VERSAO_LINGUAGEM "2.2"\nPROGRAMA Teste'
  ).replace(
    'REGRA_NEGOCIO R',
    'REGRA_NEGOCIO R\nIDENTIFICADOR_REGRA: "BR-001"'
  );
  const erros = analisarFonte(fonte, true);
  assert.ok(erros.some((e) => /exige IDENTIFICADOR_REGRA e RASTREIO_REQUISITO/.test(e.mensagem)), JSON.stringify(erros));
});

test('Lint estrito — regra sem contratos formais é reprovada', () => {
  const fonte = PROGRAMA_VALIDO.replace(
    'PROGRAMA Teste',
    'VERSAO_LINGUAGEM "2.2"\nPROGRAMA Teste\nMETADADOS_ARQUITETURA\nSLO_LATENCIA_MAXIMA: "15ms"\nFIM_METADADOS'
  ).replace(
    'REGRA_NEGOCIO R',
    'REGRA_NEGOCIO R\nIDENTIFICADOR_REGRA: "BR-001"\nRASTREIO_REQUISITO: "REQ-001"'
  );
  const erros = analisarFonte(fonte, true);
  assert.ok(erros.some((e) => /sem contratos formais/.test(e.mensagem)), JSON.stringify(erros));

  // Fora do modo estrito, o mesmo programa é aceito.
  const foraDoEstrito = analisarFonte(fonte, false);
  assert.equal(foraDoEstrito.length, 0, 'Sem estrito não deve haver erros: ' + JSON.stringify(foraDoEstrito));
});

test('Analisador — MONETARIO nunca mistura moeda em expressões', () => {
  const fonte = `
PROGRAMA Teste
REGRA_NEGOCIO R
    OPERACAO Op(a: MONETARIO("BRL"), b: MONETARIO("USD")) : DECIMAL(18, 4)
    INICIO
        VARIAVEL soma : MONETARIO("BRL") <- a + b
        RETORNE 0.0
    FIM
FIM_REGRA_NEGOCIO
FIM_PROGRAMA
`;
  const erros = analisarFonte(fonte);
  assert.ok(erros.some((e) => /moedas distintas/.test(e.mensagem)), JSON.stringify(erros));
});
