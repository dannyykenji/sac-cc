# SAC-CC — Sistema de Agendamento da Coordenação

> Sistema web completo (full-stack) para agendamento e acompanhamento de atendimentos da coordenação de Ciência da Computação: horários online/presenciais, registro de resultados, chat em tempo real, auditoria e painel de indicadores.

**[Java 17](https://openjdk.org/) · [Spring Boot 3.5](https://spring.io/projects/spring-boot) · [React 19](https://react.dev/) + TypeScript + Tailwind · [PostgreSQL 18](https://www.postgresql.org/) · WebSocket/STOMP · Docker**
---
Aviso: Esta aplicação está hospedada no plano gratuito do Render. Por isso, o servidor pode levar alguns instantes para responder ao primeiro acesso, pois pode entrar em modo de inatividade após um período sem utilização. Aguarde um momento enquanto a aplicação é inicializada.
---

### Contas de seed (contas para teste, como não tenho login do aluno do canvas)
| E-mail | Senha | Perfil |
|---|---|---|
| `admin@saccc.br` | `Admin@1234` | ADMIN |
| `coordenacao@saccc.br` | `Coord@1234` | COORDENADOR |
| `aluno@saccc.br` | `Aluno@1234` | ALUNO |
---

##  Destaques de engenharia

- **Corrida de concorrência tratada de verdade** — dois agendamentos simultâneos do mesmo aluno são serializados com `SELECT ... FOR UPDATE` **nativo** na linha do usuário (JPQL pessimista não trava em herança JOINED). **Provado no PostgreSQL real**: 2 threads HTTP simultâneas → `1×201 + 1×409`, banco com exatamente 1 registro. Três camadas de prevenção de duplicidade (índice único parcial + lock do slot + lock do aluno — ADR-005).
- **Chat em tempo real** (ADR-008): sala 1:1 por agendamento, WebSocket + STOMP sobre SockJS (`/ws`), envio idempotente via REST (`POST /api/agendamentos/{id}/chat`), append-only, janela de funcionamento parametrizada (abre 10 min antes, fecha 15 min depois), isolamento entre salas verificado.
- **Artefato único de deploy** (ADR-009): build multi-stage empacota a SPA dentro do jar do Spring Boot — API + UI + WebSocket na mesma origem, um container + PostgreSQL via compose, fallback de rotas do React Router no backend.
- **78 testes automatizados** (integração + unidade, H2 modo PostgreSQL): segurança, antecedência mínima, limite de agendamentos, prazo de cancelamento, janela do chat, append-only, isolamento 404, corrida do mesmo aluno. Testes imunes ao horário de execução (bug latente de "+2h cruzando meia-noite" descoberto e corrigido).
- **Regras de negócio parametrizáveis** em banco (`parametro_sistema`): antecedência, prazo de cancelamento, limite de agendamentos futuros — mudar regra = `UPDATE`, não deploy.
- **Auditoria e acessibilidade**: trilha completa de ações sensíveis; WCAG AA verificado matematicamente em todos os pares de cor do tema, 12 telas auditadas em viewport mobile real (390px) sem overflow horizontal, zoom 200%, navegação por teclado com foco visível.

##  Segurança

| Aspecto | Implementação |
|---|---|
| Autenticação | Sessão de servidor com **cookie httpOnly** + **CSRF token** para SPA (padrão Spring Security para essa topologia — não JWT em localStorage) |
| Senhas | **BCrypt** (hash + salt) |
| Força bruta | Bloqueio após 5 falhas (`429` + `Retry-After`) |
| Autorização | 3 perfis (ADMIN/COORDENADOR/ALUNO), 8 permissões finas, `@PreAuthorize` na camada de serviço + guarda de rotas no front |
| Autorização de objeto | Dono ou perfil com permissão — acesso a recurso de terceiros retorna **404 neutro** (não vaza existência) |
| Auto-cadastro | Conta nasce **PENDENTE**; só entra após aprovação de admin |
| WebSocket | Handshake aberto, mas **autorização por sessão no broker** e isolamento por sala |
| Segredos | Senha do banco via variável de ambiente, nunca no repositório |
| Falsos positivos | Requisição sem token CSRF → 403; rota protegida anônima → 403 (entry point) |

##  Como rodar

Requisitos completos, configuração manual e troubleshooting: **[`docs/GUIA_EXECUCAO_TESTES.md`](docs/GUIA_EXECUCAO_TESTES.md)**.

### Containers (qualquer máquina com Docker/Podman)
```bash
export SACCC_DB_PASSWORD='senha-forte'      # nunca comitar
docker compose up -d --build                # app + PostgreSQL 18
curl localhost:8080/actuator/health         # → {"status":"UP"}
```
Abrir **http://localhost:8080** — SPA, API e WebSocket na mesma porta. Dados persistem no volume `pgdata`.

### Desenvolvimento (hot-reload)
```bash
# terminal 1 — backend
cd backend && ./mvnw spring-boot:run                 # :8080

# terminal 2 — frontend
cd frontend && npm install && npm run dev            # :5173 (proxy /api e /ws)
```

### Testes automatizados
```bash
cd backend  && ./mvnw verify      # 78 testes (H2 em memória, sem infra externa)
cd frontend && npx tsc --noEmit && npm run build
```

##  Funcionalidades por perfil

| Aluno | Coordenação | Admin |
|---|---|---|
| Consulta horários disponíveis | Publica/bloqueia disponibilidade (lotes de 10–240 min) | Aprova auto-cadastros pendentes |
| Agendamento em wizard 3 passos (protocolo de acompanhamento) | Agenda de atendimentos com filtros + paginação | Gerencia usuários (ativar/desativar/perfis) |
| Cancelamento com motivo (respeitando prazo) | Registra atendimento (Resolvido / Não resolvido / Em tratativa) + histórico | Trilha de auditoria completa |
| Chat em tempo real com a coordenação | Painel de estatísticas (resumo, evolução, distribuição) | — |
| Histórico de atendimentos | Aprova auto-cadastros | — |

**Auto-cadastro**: aluno cria conta → PENDENTE → aprovação da coordenação/admin → acesso liberado (ADR-006).

##  Documentação de engenharia

| Documento | Conteúdo |
|---|---|
| [`docs/PROMPT_MESTRE.md`](docs/PROMPT_MESTRE.md) | Regras que governam o desenvolvimento (padrões, definitudes, testes) |
| [`docs/PLANO_DE_EXECUCAO.md`](docs/PLANO_DE_EXECUCAO.md) | Fases 0–6 com status, decisões e evidências |
| [`docs/CRITERIOS_DE_ACEITE.md`](docs/CRITERIOS_DE_ACEITE.md) | Ensaio formal dos 12 critérios de aceite dos requisitos |
| [`docs/GUIA_EXECUCAO_TESTES.md`](docs/GUIA_EXECUCAO_TESTES.md) | Execução por modo + roteiro manual de testes por perfil |
| [`docs/adr/`](docs/adr/) | **9 ADRs**: stack, SPA, PostgreSQL/H2/Flyway, auth local, anti-duplicidade, auto-cadastro, identidade visual, chat, dockerização |
| [`docs/design/MODELO_DE_DADOS.md`](docs/design/MODELO_DE_DADOS.md) · [`API_FASE1.md`](docs/design/API_FASE1.md) · [`TELAS_SPA.md`](docs/design/TELAS_SPA.md) | Modelo de dados, API e telas |
| [`docs/padroes/CONVENCOES.md`](docs/padroes/CONVENCOES.md) | Convenções de código, API, testes e commits |
| [`docs/DECISOES_ABERTAS.md`](docs/DECISOES_ABERTAS.md) | Pendências de decisão com a coordenação (#N) e TI (#T) |

##  Arquitetura (resumo)

```
┌──────────────────────────── container app ────────────────────────────┐
│  Spring Boot 3.5 (Java 17)                                            │
│  ├─ SPA React buildada (static/, mesma origem — ADR-009)              │
│  ├─ REST API  /api/**          ← ProblemDetail RFC 7807 em pt-BR      │
│  ├─ WebSocket  /ws (SockJS+STOMP)  → broker por sala de agendamento   │
│  ├─ Spring Security: sessão httpOnly + CSRF + BCrypt + @PreAuthorize  │
│  ├─ Spring Data JPA (herança JOINED de usuários) + locks pessimistas  │
│  └─ Flyway: migrations versionadas (V001–V004+, específicas de PG)    │
└────────────────────────────────┬──────────────────────────────────────┘
                                 │ JDBC (Hikari)
                        ┌────────▼────────┐
                        │  PostgreSQL 18  │  volume persistente
                        │  (container)    │  parâmetros do sistema em tabela
                        └─────────────────┘
```

- **API documentada** (springdoc-openapi): `http://localhost:8080/swagger-ui.html`
- Erros padronizados **ProblemDetail (RFC 7807)** em português
- Fuso do sistema **configurável** via env `SACCC_TIMEZONE` (padrão `America/Sao_Paulo`; ex.: `America/Manaus` — RNF62). Todas as comparações de horário (chat, prazos, validações) usam o mesmo relógio da instituição.

##  Stack detalhada

| Camada | Tecnologias |
|---|---|
| Backend | Java 17 (`--release`), Spring Boot 3.5, Spring Security, Spring Data JPA, Flyway, springdoc-openapi, WebSocket/STOMP |
| Frontend | React 19, TypeScript estrito, Vite, Tailwind CSS 4, React Router, TanStack Query |
| Banco | PostgreSQL 18 (dev/prod) · H2 em modo PostgreSQL (testes) |
| Infra | Dockerfile multi-stage · docker-compose · Podman rootless |
| Qualidade | 78 testes de integração · verificação WCAG AA · auditoria mobile 12 telas |

##  Números

| | |
|---|---|
| Testes automatizados | **78 verdes** |
| Commits | 33 (histórico linear e convencionado) |
| ADRs | 9 decisões documentadas |
| Requisitos atendidos | 65 RFs · 62 RNFs · 15 RNs · 11 UCs (MVP da seção 8 + aceite da seção 9) |
| Capturas de tela | 20 (desktop + mobile 390px) |

## Fonte dos requisitos

Documento oficial fornecido pela coordenação do curso: 65 requisitos funcionais, 62 não-funcionais, 15 regras de negócio e 11 casos de uso — as seções 8 (escopo MVP) e 9 (critérios de aceite) orientam o plano e o ensaio de aceite.
