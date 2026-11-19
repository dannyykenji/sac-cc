# ADR-003 — Persistência: PostgreSQL (dev/prod) + Flyway; H2 nos testes

**Status:** ACEITA (escolha do usuário em 2026-10-10)
**Data:** 2026-10-10

## Contexto

- RNF29–31: backup, restauração e integridade dos dados — banco institucional sério.
- RNF14: índices para consultas por data, status, aluno, modalidade e categoria.
- RNF57–60: transações, integridade referencial, sem exclusões físicas de registros históricos.
- Máquina sem Docker e sem PostgreSQL instalados (Fase 0 instala via `dnf`).

## Decisão

1. **PostgreSQL 16+** para dev e produção (18.6 instalado via `dnf` — out/2026). Banco `saccc` e usuário aplicativo dedicado (menor privilégio, seção 7.2 do requisitos). Nota: pg_hba padrão do Fedora usa `ident` para TCP local — ajuste para `scram-sha-256` documentado na Fase 0 do plano.
2. **Flyway** versiona todo o esquema: `src/main/resources/db/migration`. Migrations escritas para PostgreSQL; os testes rodam **as mesmas migrations** em H2 (`jdbc:h2:mem:...;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE`).
3. Se um script usar sintaxe que o H2 não suporta, criar variante em `db/migration/h2` e registrá-la em `spring.flyway.locations` apenas no profile de teste — o objetivo é paridade máxima de esquema entre dev e teste.
4. `spring.jpa.hibernate.ddl-auto=validate` em dev/prod (esquema nasce só do Flyway).
5. Índices criados via migration desde a primeira tabela: agendamento (disponibilidade, status, aluno, data), atendimento (status de resolução, responsável, categoria), disponibilidade (data, status) — RNF14.
6. Sem exclusão física: desativação lógica (coluna `ativo`/status) para entidades com histórico (RNF60).

## Consequências

**Positivas**
- Esquema versionado, auditável e reproduzível; zero drift entre ambientes.
- Testes de integração rápidos, sem dependência de Docker (Testcontainers descartado).

**Negativas**
- Pequeno risco de divergência H2↔PostgreSQL em SQL avançado (mitigado pelas variantes `h2` e por manter o SQL simples e portátil).
- Constraint UNIQUE parcial (ADR-005) exige atenção: existe nativamente no PG; no H2 validar o comportamento equivalente nos testes.
