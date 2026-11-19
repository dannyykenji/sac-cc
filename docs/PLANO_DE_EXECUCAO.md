# PLANO DE EXECUÇÃO — SAC-CC

> **Decisão de execução (2026-10-22, usuário):** frontend CONGELADO a partir da Fase 4 — as telas existentes permanecem funcionando, mas **nenhuma tela nova** é construída; Fases 4 e 5 entregam **API + testes**; o frontend será refeito completamente após o backend finalizar. Verificação das fases 4–5: `./mvnw verify` apenas.

**Como usar:** cada fase tem meta, entregáveis, requisitos cobertos e verificação. Uma fase só fecha com todos os critérios verdes, commitados e revisados com o usuário. Fases não são puladas; dentro da fase, trabalhar em commits pequenos.

---

## Fase 0 — Ambiente e esqueleto
**Meta:** máquina apta a desenvolver e rodar; esqueleto dos dois projetos subindo.

- [x] Instalar/verificar JDK **com `javac`** (OpenJDK 25.0.4.1 — feito pelo usuário)
- [x] Instalar PostgreSQL (18.6 via dnf — feito pelo usuário); banco `saccc` criado
- [x] Criar `backend/` (start.spring.io + `pom.xml` rebaixado para 3.5.16, já com `mvnw`) e `frontend/` (Vite + React + TS + Tailwind)
- [x] Proxy do Vite → `localhost:8080` configurado; Swagger UI no `pom` (springdoc 2.8.6)
- [x] `./mvnw verify` verde (H2 + Flyway validate); `npm run build` verde; commits iniciais
- [x] Rodar backend contra o PostgreSQL de dev + `GET /actuator/health` → **UP** (2026-10-10; pg_hba ajustado para scram-sha-256, role/banco via pkexec; senha em `~/.saccc-env`)

> Nota: Flyway avisa que PG 18 é mais novo que o suportado testado (17) — aviso cosmético; se um migration falhar em PG 18, avaliar upgrade do flyway-database-postgresql.

**Verificação:** `./mvnw verify` ✅ · `curl localhost:8080/actuator/health` → `{"status":"UP"}` ✅ · `npm run build` ✅

## Fase 1 — Usuários, perfis e autenticação
**Cobre:** UC01, UC11, auto-cadastro (ADR-006) · RF01–RF07 · RNF15, RNF17–RNF22
**Status: backend CONCLUÍDO (2026-10-10) — 17 testes verdes + E2E real contra PG; frontend pendente**

- [x] Migração V001 (usuário/perfis/permissões/auditoria + seed) validada em H2 e PG 18
- [x] Login/logout/me com sessão cookie httpOnly + CSRF SPA (fluxo real verificado com curl)
- [x] Bloqueio temporário após 5 falhas (429 + Retry-After); 401 genérico; 403 p/ pendente/inativo
- [x] Auto-cadastro público → conta PENDENTE; resposta neutra em duplicidade; senha BCrypt
- [x] Fila de aprovação (aprovar/rejeitar com motivo) + auditoria de tudo (LOGIN, LOGIN_FALHA, CRIAR, APROVAR...)
- [x] CRUD admin de usuários: desativação lógica, 409 último admin ativo, 409 e-mail duplicado
- [x] **Frontend**: telas T01–T03 (login, auto-cadastro, aguardando aprovação), M1 (usuários), M2 (pendentes) — E2E real validado no navegador (login admin → auto-cadastro → aprovação → login do aluno aprovado)
**Testes-chave:** ✅ 16 testes de integração + contexto (0 failures) — acesso por perfil; inativo e pendente bloqueados; duplicidade neutra; senha hasheada; permissões negadas; CSRF obrigatório

## Fase 2 — Disponibilidade da agenda
**Cobre:** UC06 · RF08–RF16
**Status: CONCLUÍDA (2026-10-18) — 10 testes verdes; E2E real (coordenador cria lote, aluno consulta)**

- [x] Migração V002: tabela `disponibilidade` + índice (data, status) + ADMIN ganha `GERENCIAR_DISPONIBILIDADE`
- [x] Criação em lote (data + intervalo + duração → N slots); duração parametrizável 10–240 min (RF09, RNF42)
- [x] Conflito de horário rejeitado com 409 (RF13); data passada rejeitada (400); local obrigatório no presencial
- [x] Bloqueio/desbloqueio (RF12); horário RESERVADO não pode ser bloqueado (409)
- [x] Visão do aluno: só DISPONIVEL e futuro (RF15); visão admin com todos os status (RF14)
- [x] Frontend: C4 (gerenciar, criar lote, bloquear) e A2 (consulta com lista textual — RNF49)
**Testes-chave:** ✅ conflito rejeitado; data passada rejeitada; bloqueado some da visão do aluno; permissões (aluno 403)

## Fase 3 — Agendamento ★ fase de maior risco
**Cobre:** UC02–UC05 · RF17–RF30 · RN01–RN05, RN15 · RNF11 (ADR-005)
**Status: CONCLUÍDA (2026-10-18) — 42 testes verdes no total (15 da fase, incl. concorrência RNF40); E2E real completo; V004 (índice único parcial) aplicado no PostgreSQL de dev**

- [x] Migração V003 (agendamento, categorias #N8 default, `parametro_sistema` com #N3/#N4/#N5) + V004 em `db/postgresql/` (índice UNIQUE parcial — ADR-005 camada 1; H2 pula via `{vendor}`)
- [x] Confirmação transacional com **lock pessimista** (`SELECT FOR UPDATE` no slot) + revalidação (ADR-005 camada 2)
- [x] Protocolo único `AG-AAAA-NNNNNN` (RN04); anti-duplicação; sobreposição própria RN03; modalidade compatível RN15
- [x] Antecedência mínima 24h (#N5), limite 1 futuro ativo (#N4), cancelamento com prazo 24h e motivo (#N3) — **tudo parametrizável** (RNF42)
- [x] Cancelamento aluno (prazo) vs coordenação (motivo obrigatório); slot volta a DISPONIVEL; reagendamento impossível após cancelar de novo (409)
- [x] Isolamento: aluno vê só o próprio; detalhe de outro → 404 neutro (RNF18)
- [x] Frontend: A3 (formulário com categoria/assunto), A4 (protocolo em destaque + copiar), A5 (abas Próximos/Histórico), A7 (diálogo de cancelamento com motivo)
**Testes-chave:** ✅ concorrência 2 threads → 1 vence/1 recebe 409; duplicado impedido; RN03; RN15; #N4; #N5; #N3 (aluno fora do prazo → coordenação consegue)

## Fase 4 — Atendimento e status
**Cobre:** UC07–UC09 · RF31–RF43 · RN06–RN14
**Status: CONCLUÍDA (backend, 2026-11-04) — 54 testes verdes no total (12 da fase); frontend congelado por decisão do usuário**

- [x] Migração V005: `atendimento` (1:1 com agendamento, RN06), `historico_atendimento` (append-only), `notificacao`
- [x] UC08: registro de realização com campos condicionais — RESOLVIDO exige solução (RN07), NÃO_RESOLVIDO exige justificativa (RN09), EM_TRATATIVA exige prazo (RN08)
- [x] Ausência em categoria separada `AUSENCIA` — nunca vira resolvido/não resolvido (RN12); agendamento → NAO_COMPARECEU
- [x] UC09: transições validadas; encerrado exige justificativa (RN14); histórico acumulado sem sobrescrever (RN13)
- [x] Notificações registradas (canal SISTEMA — SMTP é #T5) em registro/mudança de status (RF56/59)
- [x] UC07: `/api/admin/agenda` com filtros (data, status, modalidade, categoria, busca) + paginação (RNF12)
- [x] RF43: endpoints de atendimento só para coordenação; observações internas fora do alcance do aluno
**Testes-chave:** ✅ RN07/08/09 rejeitam falta de campo; RN12 (ausência sem status, com status → 400); RN13 (histórico preservado: 2 entradas após transição); RN14 (encerrado sem justificativa → 400); duplo registro → 409; aluno → 403

## Fase 5 — Estatísticas
**Cobre:** UC10 · RF44–RF55 · RNF13
**Status: CONCLUÍDA (backend, 2026-11-04) — 62 testes verdes no total (8 da fase); frontend congelado por decisão do usuário**

- [x] `EstatisticaService` — fonte ÚNICA de contabilização (nenhum controller monta conta)
- [x] `/api/estatisticas/resumo` — 4 KPIs + % resolução + cancelamentos + ausências (RF44–47, RF55, RN10/12)
- [x] `/api/estatisticas/evolucao` — série mensal realizados × resolvidos (RF52/53)
- [x] `/api/estatisticas/distribuicao` — por modalidade e categoria (RF49/50)
- [x] Filtros: período, modalidade, categoria, responsável (RF48–51); SQL portável H2/PG
- [x] Drill-down já coberto pelo `/api/admin/agenda` da Fase 4 (mesmos filtros — RF54)
- [x] Autorização: `CONSULTAR_ESTATISTICAS` (aluno → 403)
**Testes-chave (RNF41):** ✅ massa conhecida bate com conta de mesa (10 realizados, 60.0%); recálculo pós-mudança reflete sem reiniciar (RNF61: 60%→70% após resolver tratativa); cancelados/ausências fora do denominador; filtros combinados; período vazio → zerado; aluno 403

## Fase 6 — Auditoria, notificações, acessibilidade e entrega
**Cobre:** RF56–RF65 · RNF44–RNF52
**Status: CONCLUÍDA (2026-11-05) — backend MVP COMPLETO; smoke E2E contra PG real**
**PÓS-MVP (2026-11-12): chat do atendimento online (ADR-008) — sala por agendamento, WebSocket+STOMP, janela parametrizada, mensagens append-only — 11 testes; total do backend: 77**

- [x] `GET /api/admin/auditoria` — trilha consultável pelo ADMIN com filtros e paginação (RF64/RNF26)
- [x] `GET /api/notificacoes` — usuário vê as próprias notificações; isolamento testado (RF56/60)
- [x] Garantia RNF25: auditoria sem dados sensíveis (teste dedicado)
- [x] README final de instalação/configuração/operação (RNF36)
- [x] Ensaio formal dos 12 critérios de aceite da seção 9 → `docs/CRITERIOS_DE_ACEITE.md`
- [x] Smoke E2E real contra PostgreSQL: login → slot → agendamento → atendimento → estatísticas (100% corretas)
- [x] Acessibilidade mobile completa e revisão fina — **auditoria concluída (2026-11-16)**: 12 telas em 390px sem overflow horizontal; contraste dos tokens verificado matematicamente (todos ≥ AA; `ink-3` escurecido para 5.28:1 sobre panel); ordem de foco lógica + `:focus-visible` global; zoom 200% sem quebra; alvos de toque ≥24px
- [ ] SMTP (#T5) e deploy (#T4): pendências externas — notificações já ficam registradas

---

## Marcos de validação com o usuário
1. Fim da Fase 1 — login e gestão de usuários funcionando
2. Fim da Fase 3 — fluxo completo do aluno (consultar → agendar → cancelar)
3. Fim da Fase 5 — painel com indicadores corretos
4. Fase 6 — entrega

## Ordem sugerida de resolução das decisões abertas
Antes da Fase 3: #N3 (cancelamento), #N4 (limite de agendamentos), #N5 (antecedência) · Antes da Fase 5: #N1/#N2 (realizado e % resolução) · Antes da Fase 6: #T5 (SMTP). Ver `DECISOES_ABERTAS.md`.
