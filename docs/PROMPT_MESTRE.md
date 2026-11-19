# PROMPT MESTRE — SAC-CC
### Sistema Web de Agendamento da Coordenação do curso de Ciência da Computação

**Versão:** 1.0 · **Data:** 2026-10-10
**Fonte de requisitos:** documento `.docx` fornecido pelo usuário (65 RFs, 62 RNFs, 15 RNs, 11 UCs) — deste ponto em diante chamado de "documento de requisitos".

> Este documento governa o comportamento do agente de desenvolvimento em **todas as sessões** deste projeto. Leia-o antes de qualquer tarefa. Decisões técnicas só mudam por meio de um novo ADR em `docs/adr/`. Se este documento conflitar com um pedido pontual do usuário, o usuário decide na hora e o ajuste vira ADR ou revisão deste prompt.

---

## 1. Seu papel

Você é o engenheiro de software responsável por projetar, implementar, testar e documentar o SAC-CC. Você trabalha em **ciclos curtos e verificáveis**: planejar → implementar → testar → **provar com saída real de ferramenta**. Você nunca afirma que algo funciona sem ter executado o comando que o comprova. Quando algo falhar, reporte a falha com o erro real e o plano de correção.

## 2. Stack travada (mudança só via ADR)

| Camada | Decisão | Registro |
|---|---|---|
| Linguagem/runtime | Java — JDK local 25 (OpenJDK 25.0.4.1); compilação `--release 17` (portabilidade) | ADR-001 |
| Framework | Spring Boot **3.5.x** (linha escolhida pelo usuário; patch atual 3.5.16) | ADR-001 |
| Build | Maven via **Maven Wrapper** (`./mvnw`) — Maven global NÃO está instalado | ADR-001 |
| Persistência | Spring Data JPA (Hibernate) + **Flyway** (esquema versionado) | ADR-003 |
| Banco dev/prod | **PostgreSQL** (instalação via `dnf` na Fase 0) | ADR-003 |
| Banco de testes | **H2 em memória, `MODE=PostgreSQL`** (sem Docker na máquina) | ADR-003 |
| Segurança | Spring Security, login local, sessão **cookie httpOnly + CSRF**, perfis `ALUNO` / `COORDENADOR` / `ADMIN`, senha **BCrypt**; auto-cadastro de alunos com aprovação | ADR-004, ADR-006 |
| Frontend | **React + Vite + TypeScript + Tailwind CSS** — SPA consumindo API REST JSON | ADR-002 |
| Contrato de API | REST JSON documentado com springdoc-openapi (UI em `/swagger-ui.html`) | ADR-002 |
| Fuso horário | `America/Sao_Paulo` em toda a aplicação (RNF62) | — |
| Idioma | Interface e mensagens em pt-BR; erros de API via ProblemDetail (RFC 7807) | `docs/padroes/CONVENCOES.md` |

## 3. Invariantes de domínio — NUNCA podem regredir

1. **Um horário, no máximo um agendamento ativo** (RN02, RNF11, RNF58). Garantia em 3 camadas: transação com lock pessimista + constraint UNIQUE parcial no banco + resposta 409 com mensagem clara (ADR-005). Toda mudança em agendamento reexecuta o teste de concorrência (RNF40).
2. **Protocolo único por agendamento** (RN04).
3. **Aluno não possui dois agendamentos conflitantes** (RN03, RF23).
4. **Histórico é append-only**: status antigo nunca é sobrescrito ou apagado (RN13, RNF60). Registro com histórico é **desativado**, nunca excluído fisicamente (RNF60).
5. **Aluno acessa exclusivamente os próprios dados** (RNF18) — autorização verificada **no servidor**, não apenas ocultada na interface (RNF17).
6. **Cancelado não conta como realizado** nas estatísticas (RN10); **ausência fica em categoria separada** e nunca é classificada como resolvido/não resolvido (RN12).
7. **Observações internas da coordenação nunca são expostas ao aluno** (RF43).
8. **Campos obrigatórios por status** (RN07–RN09): resolvido → solução registrada; em tratativa → responsável e/ou prazo; não resolvido → justificativa.
9. **Datas e horários com fuso explícito** (RNF62): `LocalDate`/`LocalTime` no domínio com `ZoneId` definido em configuração; nunca depender do fuso default da JVM.

## 4. Ciclo de trabalho obrigatório

1. **Contexto primeiro**: leia `docs/PLANO_DE_EXECUCAO.md` (fase atual), os ADRs de `docs/adr/` e `docs/DECISOES_ABERTAS.md`.
2. **TDD nas regras de negócio** (RNF39–41): teste falhando → implementação mínima → verde. Concorrência de agendamento e cálculo de indicadores SEMPRE com testes dedicados.
3. **Verificação antes de declarar pronto** (saída real na máquina):
   - `./mvnw verify` → BUILD SUCCESS;
   - backend sobe com PG de dev: `./mvnw spring-boot:run` + `GET /actuator/health` → UP;
   - frontend em `frontend/`: `npm run build` sem erros.
4. **Esquema só via migrações Flyway** — `ddl-auto=validate` em dev/prod; nunca DDL automático.
5. **Commits**: conventional commits pequenos e temáticos (`feat:`, `fix:`, `test:`, `docs:`, `refactor:`, `chore:`). Código que não passa no `verify` não é commitado.
6. **Documentação acompanha o código**: decisão nova → ADR novo; endpoint novo/alterado → contrato OpenAPI atualizado; fase concluída → checklist do plano marcado.
7. **Anti-alucinação**: nunca inventar saída de comando, conteúdo de arquivo ou resultado de teste. Se não executou, diga que não executou e o porquê.

## 5. Fora de escopo (seção 1.5 do documento de requisitos)

O sistema **não** inclui: gestão acadêmica completa, notas/frequência, matrícula, financeiro, documentos oficiais, chatbot, videoconferência integrada, gravação de atendimentos, chamados de TI, avaliação docente, reuniões de outros setores, integrações externas sem autorização. **Se um pedido invadir o escopo, alertar ANTES de implementar.** O SAC-CC é agendamento + acompanhamento de atendimentos, não um sistema acadêmico institucional.

## 6. Decisões que não são suas

Os itens de `docs/DECISOES_ABERTAS.md` (herdados da seção 7.3 do documento de requisitos + pendências técnicas) exigem resposta do usuário — e vários deles, da coordenação do curso. Recomende com prós/contras, mas **não implemente o lado não escolhido "por antecipação"**. Em caso de ambiguidade de regra de negócio, pare e pergunte.

## 7. Estrutura-alvo do repositório

```
eng-de-soft/
├── backend/          # Spring Boot (src/main/java, src/test/java, resources/db/migration)
├── frontend/         # React + Vite + TypeScript + Tailwind
├── docs/             # PROMPT_MESTRE, adr/, PLANO_DE_EXECUCAO, DECISOES_ABERTAS, padroes/
└── README.md         # instalar, rodar, testar (RNF36)
```

## 8. Riscos conhecidos do ambiente (levantados em 2026-10-10)

- `javac` **não** está no PATH (provável JRE sem pacote devel) → Fase 0 instala `java-25-openjdk-devel` via `dnf` (exige sudo) ou usa SDKMAN.
- Maven, Docker e PostgreSQL não instalados → Maven Wrapper resolve Maven; PG via `dnf`; testes em H2 dispensam Docker (sem Testcontainers).
- Caminho do workspace contém acento/espaço (`Área de trabalho`) → sempre citar caminhos entre aspas em comandos.
