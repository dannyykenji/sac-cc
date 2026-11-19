# ADR-009 — Dockerização e artefato único para deploy (#T4)

**Status**: aceito (2026-11-18)
**Contexto**: #T4 exigia forma reprodutível de rodar o SAC-CC em qualquer máquina/servidor. Instalação manual (README) funciona mas é sensível a versões locais (JDK, Node, PG) e trabalhosa. Além disso, em dev o Vite (5173) servia a SPA separada do backend (8080), exigindo proxy — desnecessário em produção.
**Decisão**:
1. **Imagem única multi-stage** (`Dockerfile`): node:20 builda a SPA → maven/temurin-17 empacota o jar com a SPA embutida em `static/` (perfil Maven `with-frontend`) → JRE 17 Alpine executa como usuário não-root, com `MaxRAMPercentage=75` e TZ America/Sao_Paulo. Imagem ~254 MB. Imagens fully-qualified (`docker.io/library/...`) para compatibilidade Podman rootless.
2. **Orquestração** (`docker-compose.yml`): app + postgres:18-alpine com volume persistente e healthcheck; senha via env `SACCC_DB_PASSWORD` (nunca no git). Funciona com `docker compose` ou `podman compose` (sem plugin: `podman network create` + 2× `podman run`, documentado no README).
3. **Mesma origem**: backend passa a servir a SPA. `SpaConfig` (resource handler `/**` → `classpath:/static/` com fallback para `index.html` nas rotas de navegação — sem isso o React Router recebe 404 em refresh/direct-link) e `SecurityConfig` libera o **shell** público (`/`, `/login`, `/cadastro*`, `/app/**`, `/assets/**`, `/favicon.ico`): HTML/JS/CSS não carregam dados; **todas** as chamadas `/api/**` continuam exigindo sessão + CSRF. WebSocket `/ws` na mesma origem (sem proxy).
**Consequências**:
- Deploy = `docker compose up -d --build` em qualquer host com Docker/Podman; porta única 8080.
- Dev continua com Vite 5173 (HMR) — os dois modos convivem.
- Bug latente corrigido de graça: testes de Fase3 que criavam slot "agora+2h" quebravam o CHECK `hora_fim > hora_inicio` quando rodavam após ~21:30 (virava 00:xx) → helper `slotBreveMenosDe24h()` (amanhã 10:00).
- Armadilha descoberta: resource handler `/**` **sem** `addResourceLocations` sombreia o static do Spring Boot → 404 → forward `/error` → 403 enganoso. Location explícita obrigatória.
