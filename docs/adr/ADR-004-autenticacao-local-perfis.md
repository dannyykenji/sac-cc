# ADR-004 — Autenticação local com perfis; extensível para SSO institucional

**Status:** ACEITA (escolha do usuário em 2026-10-10; mecanismo de sessão decidido no mesmo dia — ver ponto 7)
**Data:** 2026-10-10

## Contexto

- O documento de requisitos (seção 2.1.5) menciona possível login institucional, mas não há provedor definido nem credenciais (seção 7.3.11).
- RF01–RF07: autenticação, perfis distintos, bloqueio de inativos, recuperação de senha, permissões.
- O usuário escolheu login local com perfis para o MVP, deixando a arquitetura pronta para plugar SSO depois.

## Decisão

1. **Spring Security** com autenticação local (usuário + senha no banco).
2. Perfis: **ALUNO**, **COORDENADOR**, **ADMIN** (administrador do sistema). O "usuário administrativo" do requisitos é coberto pelo ADMIN com permissões granulares.
3. O modelo `PerfilAcesso` + `Permissao` (classes 3.5/3.6 do requisitos) vira tabelas; a autorização nos endpoints usa `@PreAuthorize` **por permissão** (ex.: `GERENCIAR_DISPONIBILIDADE`), não apenas por perfil — RF06.
4. Senhas com **BCrypt** (RNF15). Bloqueio temporário após N tentativas inválidas (fluxo alternativo do UC01, RNF22) com registro em log/auditoria.
5. Isolamento: toda autenticação passa por um `AuthenticationProvider` próprio → trocar por OAuth2/OIDC institucional no futuro não altera o domínio.
6. Recuperação de senha (RF04) só entra quando houver SMTP definido (#T5).
7. **Sessão da SPA (#T1, decidido em 2026-10-10): cookie httpOnly + SameSite=Lax + token CSRF**, usando o suporte SPA do Spring Security. Justificativa: cookie ilegível para JavaScript → credencial nunca exposta a XSS (RNF20); logout/revogação imediatos no servidor (RNF21, RF03 — desativar usuário corta o acesso na hora); deploy same-origin (ADR-002) elimina CORS. **JWT stateless foi considerado e rejeitado**: seus benefícios (multi-domínio, consumidores terceiros da API, stateless puro) não se aplicam a um monolito same-origin, e os custos (token roubável via `localStorage`, revogação exigindo blacklist/refresh) conflitam com RNF20/21. Reavaliar somente se surgir app mobile ou API aberta a terceiros (novo ADR).

## Consequências

**Positivas**
- MVP independente de serviços externos; LGPD-friendly (RNF24: dados mínimos, hash, trilha).
- Caminho claro para SSO sem reescrita.

**Negativas**
- Gestão de senhas/recuperação fica como responsabilidade nossa até o SSO existir.
- Duas superfícies de segurança (sessão + permissões) para testar desde a Fase 1.
