# ADR-002 — Frontend SPA React + Vite + Tailwind consumindo API REST

**Status:** ACEITA (escolha do usuário em 2026-10-10)
**Data:** 2026-10-10

## Contexto

- RNF08/54/55: interface responsiva em computadores, tablets e smartphones.
- RNF44–52: acessibilidade (teclado, foco visível, rótulos, contraste, alternativa textual ao calendário).
- RNF02: o aluno consulta disponibilidade e agenda atravessando no máximo 5 telas principais.
- O usuário escolheu SPA React consumindo API REST, com contrato documentado (OpenAPI), em vez de Thymeleaf.

## Decisão

1. **SPA React + Vite + Tailwind CSS** em `frontend/` (Node 26.7 e npm 11 já presentes na máquina).
2. Bibliotecas base: **React Router** (rotas), **TanStack Query** (estado de servidor/cache/reativação), **react-hook-form + zod** (formulários com validação espelhando a Bean Validation do backend), **axios** com interceptor de autenticação.
3. Contrato: springdoc-openapi expõe `/v3/api-docs`; o frontend consome endpoints versionados sob `/api`; mudanças de contrato são registradas no plano da fase.
4. Desenvolvimento: Vite dev server com **proxy** para `http://localhost:8080` (mesma origem efetiva, sem CORS em dev).
5. Produção (recomendação a confirmar em #T4): `npm run build` copiado para `src/main/resources/static` do Spring Boot → **artefato único**, mesma origem, sem CORS.
6. Linguagem: **TypeScript** (decisão do usuário — #T3, 2026-10-10). Tipos dos contratos derivados do OpenAPI sempre que possível.
7. Acessibilidade desde o primeiro componente: rótulos associados (RNF46), foco visível (RNF45), navegação por teclado (RNF44), mensagens de status em texto além de cor (RNF47/50), calendário com estrutura textual alternativa (RNF49).

## Consequências

**Positivas**
- Contrato de API explícito e testável; front e back evoluem de forma independente.
- Ecossistema React maduro para calendários, tabelas e gráficos do painel de estatísticas.

**Negativas**
- Duas bases de código e dois builds (Maven + npm) — o ciclo de verificação do PROMPT_MESTRE exige ambos.
- State management e cache do lado cliente passam a ser responsabilidade nossa (mitigado pelo TanStack Query).
