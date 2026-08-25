# Instruções do Copilot para THZ-LANG

## Regra global obrigatória

Toda alteração de código no repositório THZ-LANG deve incluir testes unitários automatizados antes ou junto com a implementação.

Esta é uma regra rígida e aplica-se a todo o projeto, incluindo Java, TypeScript, runtime, CLI, GUI, LSP e ferramentas auxiliares.

## Padrões obrigatórios

1. Nenhuma correção, funcionalidade nova, refatoração ou ajuste de comportamento deve ser entregue sem testes unitários.
2. Quando houver correção de defeito, o teste deve reproduzir o problema e validar a correção.
3. Testes devem cobrir o caminho feliz e pelo menos um caso de borda ou falha esperada.
4. Em módulos Java, prefira JUnit 5 e mantenha os testes próximos do código alterado.
5. Em módulos TypeScript, prefira testes automáticos com Node Test Runner ou framework equivalente já adotado no projeto.
6. Para mudanças de parser, lexer, semântica, runtime, contratos e regras de negócio, os testes devem verificar comportamento funcional observável, não apenas mocks.
7. Se a mudança afetar comportamento público, a suíte relevante deve ser executada e o resultado informado.

## Regras de qualidade

- Escreva testes claros, legíveis e em português do Brasil.
- Use nomes de teste descritivos que expressem o cenário e a expectativa.
- Não confie apenas em validação manual; testes automatizados são parte essencial da entrega.
- Quando a mudança for complexa, divida em mais de um teste para separar cenários distintos.
- Em casos de regressão, adicione teste de regressão específico.

## Exemplo de comportamento esperado

Ao receber uma tarefa como "corrigir bug no lexer" ou "adicionar validação de contrato", o agente deve:

1. localizar a área afetada;
2. escrever ou ajustar o teste unitário reproduzindo o cenário;
3. implementar a correção;
4. executar a suíte relevante;
5. reportar o resultado com evidência.

## Resumo de política

Não é aceitável entregar código sem teste unitário correspondente. A ausência de testes é uma falha de qualidade e deve ser tratada como parte do trabalho.
