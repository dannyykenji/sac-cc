# ADR-006 — Auto-cadastro de alunos com aprovação

**Status:** ACEITA (escolha do usuário — #T6, 2026-10-10)
**Data:** 2026-10-10

## Contexto

- O requisitos (premissa 7.1) admite que alunos "tenham conta institucional ou possam ser cadastrados no sistema"; RF05/RF07 exigem cadastro de usuários e dados mínimos do aluno (nome, matrícula, curso, e-mail).
- Opções para #T6: cadastro manual pelo admin · carga em lote via planilha · **auto-cadastro com aprovação**.
- O usuário escolheu **auto-cadastro** para o MVP. Este ADR registra a interpretação adotada: auto-cadastro **com aprovação** (como apresentado na opção), porque o cadastro livre não consegue garantir que quem se registra é aluno matriculado — a matrícula informada precisa de validação humana contra a lista institucional.

> **Se o usuário preferir acesso imediato sem aprovação**: remover o passo de aprovação e criar contas já ativas; o restante deste ADR permanece válido.

## Decisão

1. **Endpoint público** `POST /api/cadastro-aluno` (sem autenticação) com: nome, e-mail, matrícula, curso (pré-definido CC), senha. Bean Validation completa; matrícula e e-mail únicos (UC11: duplicidade impedida).
2. A conta nasce com status **PENDENTE** — inativo para efeitos de login (RF03, RN01: só aluno ativo agenda).
3. **ADMIN aprova ou rejeita** o cadastro no painel, validando a matrícula. Coordenador pode receber essa permissão via `PerfilAcesso`/`Permissao` (arquitetura de ADR-004 permite sem mudança de código de autorização).
4. Aprovação ativa a conta e libera o login; toda ação de aprovação/rejeição é auditada (RF64) e o registro de criação preserva quem fez (auto-cadastro marca `criadoPor = próprio usuário, via auto-cadastro`).
5. Endpoint público protegido: rate limit por IP + resposta genérica em caso de e-mail/matrícula já existente (não vazar existência de contas — RNF25) e sem dados pessoais em log.
6. Carga em lote via planilha fica registrada como evolução posterior (não no MVP).

## Consequências

**Positivas**
- Onboarding sem gargalo do admin; coordenação mantém controle de quem é aluno de fato.
- Fluxo coerente com UC01 (pré-condição "usuário ativo") e RN01 sem regra nova.

**Negativas**
- Sobra uma fila de aprovação para construir na Fase 1 (UI + endpoint + auditoria).
- **Sobe a prioridade de #T5/#N9** (SMTP): aluno que define a própria senha esquece mais do que aluno com senha provisionada — sem SMTP, recuperação fica administrativa (admin redefine).
- Critério de validação da matrícula (formato/lista oficial) precisa ser informado pela coordenação — pendência junto a #N8.
