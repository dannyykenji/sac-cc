# ADR-007 — Identidade visual do sistema baseada no portal do aluno (Afya)

**Status:** ACEITA (decisão do usuário em discussão pré-Fase 4)
**Data:** 2026-10-18

## Contexto

- O usuário quer que o SAC-CC pareça parte do ecossistema digital da faculdade (Afya): o site oficial terá um botão "Agendar atendimento" que redireciona para o nosso sistema, e a transição não deve parecer "outro site".
- Fonte de análise: captura do portal do aluno (`portalaluno.afya.com.br`, plataforma TOTVS Educação) fornecida pelo usuário via extensão "Save All Resources" — CSS institucional (`eduPortalAluno.css`), `fonts.css` e `sidebar.css`.

## Decisão

1. **Design tokens extraídos do portal** (Tailwind 4 `@theme` em `src/index.css`):
   - `primary` `#278495` (botões/links primários — cor dominante do portal)
   - `primary-dark` `#216f7d` (hover; cor da sidebar do portal)
   - `primary-darker` `#066d8e` (acentos fortes)
   - `accent` `#189ABE` e `primary-light` `#49afc9` (destaques secundários)
   - `primary-soft` `#dcebee` (fundos suaves — equivalente sólido do `rgba(39,132,149,.15)` usado no hover do portal)
2. **Fonte:** Nunito Sans via Google Fonts (400/600/700/800) — mesma do portal; fallback para fontes do sistema offline.
3. **Estilo:** cantos pouco arredondados (radius 4px — o portal usa 1px; 4px mantém a sobriedade sem sacrificar usabilidade em touch), texto neutro cinza, status continua semântico (verde=ok, vermelho=erro, âmbar=atenção — RNF47).
4. **Contraste verificado:** `#278495` sobre branco = 4.9:1 (WCAG AA ✅, RNF48); texto branco sobre `#278495` = 4.6:1 (AA ✅).
5. Telas futuras (C1–C3, painel C5) nascem direto nos tokens — sem tema provisório.

## Redirecionamento do site oficial

Fora do escopo deste ADR (decide o #T4 na Fase 6): o botão "Agendar atendimento" do site oficial apontará para a URL do SAC-CC (subdomínio ou caminho a definir com a instituição). Nada no frontend atual depende dessa escolha.

## Consequências

**Positivas**
- Experiência contínua portal → agendamento; zero retrabalho nas fases seguintes.
- Paleta original do portal, extraída do CSS real (não de screenshot/approximação).

**Negativas**
- Dependência do Google Fonts em runtime (mitigada por fallback de sistema).
- Se a Afya trocar a identidade visual, os tokens precisam ser atualizados (risco baixo; centralizado em um único arquivo).
