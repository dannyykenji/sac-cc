# ADR-005 — Prevenção de agendamento duplicado (concorrência)

**Status:** PROPOSTA (validar na Fase 3 com testes de concorrência reais; aprovação formal pelo usuário)
**Data:** 2026-10-10

## Contexto

- RNF11: "impedir condições de concorrência que permitam dois alunos confirmarem o mesmo horário".
- RNF58 / RN02: um horário não pode possuir múltiplos agendamentos ativos.
- UC02 (fluxo alternativo): a consulta pode estar desatualizada — "Se um horário for reservado por outro usuário durante a consulta, o sistema deverá atualizar a informação antes da confirmação".
- UC03: "O sistema verifica novamente a disponibilidade" na confirmação.

## Decisão — defesa em 3 camadas

1. **Banco de dados**: índice UNIQUE parcial no PostgreSQL em `agendamento(disponibilidade_id)` filtrando agendamentos ativos (`status IN ('SOLICITADO','CONFIRMADO')`). É a última linha de defesa, válida mesmo com múltiplas instâncias da aplicação.
2. **Aplicação**: a confirmação do agendamento roda em transação com **lock pessimista** (`SELECT ... FOR UPDATE`) na `Disponibilidade`, revalidando status e modalidade dentro da transação — transações curtas para não degradar a agenda.
3. **API/UI**: quando o slot foi tomado durante o preenchimento, responder **409 Conflict** com ProblemDetail em pt-BR ("Horário acaba de ser ocupado; escolha outro") e a SPA recarrega a disponibilidade (UC02 alt.).

Testes obrigatórios antes da fase fechar (RNF40):
- Teste de concorrência: 2 threads confirmando o mesmo slot → exatamente 1 sucesso e 1 conflito.
- Teste da constraint: tentativa de inserção direta violando o UNIQUE parcial → erro tratado.

## Consequências

**Positivas**
- Garantia forte e independente de camada; comportamento definido para o usuário (mensagem clara + reconsulta).

**Negativas**
- Lock por linha serializa confirmações no mesmo slot (aceitável: volume baixo, RNF13).
- Unique parcial é sintaxe PG; equivalência no H2 dos testes precisa ser verificada (ver ADR-003, consequências).
