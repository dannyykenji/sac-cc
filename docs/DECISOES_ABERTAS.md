# DECISÕES ABERTAS — SAC-CC

Duas frentes: **#N (negócio)** — respondidas pela coordenação/usuário; **#T (técnica)** — pelo usuário. Nenhuma decisão é tomada "por antecipação" pelo agente (regra do PROMPT_MESTRE §6).

## Negócio — ✅ TODAS RESPONDIDAS (2026-11-19)

| # | Decisão | Implementação |
|---|---|---|
| **#N1** | **(c) Confirmação dupla** — "realizado" conta só quando aluno E coordenação confirmam | `atendimento.confirmado_aluno` (V008) + `POST /api/atendimentos/{id}/confirmar-aluno` (dono, idempotente, 404 neutro) + botão "Confirmar comparecimento" no histórico do aluno + estatísticas exigem `realizado AND confirmado_aluno` |
| **#N2** | **Manter** — % resolução = resolvidos / realizados (cancelados e ausências fora) | Inalterado (já era o comportamento) |
| **#N3** | **Confirmado os 3** — prazo 24h parametrizável; motivo obrigatório (ambos); slot liberado automaticamente | Inalterado (já era o comportamento) |
| **#N4** | **Manter 1** agendamento futuro ativo por aluno | Inalterado (`LIMITE_AGENDAMENTOS_FUTUROS=1`) |
| **#N5** | **2 horas** de antecedência mínima (era 24h) | V008 `UPDATE parametro_sistema` + testes novos (<2h → 400; >2h → 201) |
| **#N6** | **Sala automática, sem link externo** — online usa o próprio chat (ADR-008) | Campo "Link da reunião" removido do formulário; nota de sala automática; validação antiga de link deprecated |
| **#N7** | **Local fixo da coordenação** para presencial quando o horário não informar local | Parâmetro `LOCAL_PRESENCIAL_FIXO` (V008) + `DisponibilidadeService` aplica automaticamente + teste |
| **#N8** | **Manter** as 10 categorias da classe 3.10 | Inalterado |
| **#N9** | **Sem SMTP por enquanto** (recuperação de senha segue administrativa). NOVIDADE: lembrete no sino quando atendimento ONLINE está a ≤15min do início, para aluno E coordenação | `LembreteChatService` — job `@Scheduled` de 1min, parâmetro `LEMBRETE_CHAT_MIN_ANTES=15` — **provado E2E nas duas direções**. Notificações existentes mantidas |
| **#N10** | **Deploy de avaliação com chat aberto desde a criação** — sala ABERTA desde a criação do agendamento, com aviso visível de que vale só para teste; janela real (10min antes / 15min depois) segue parametrizada | Parâmetro `CHAT_MODO_TESTE='true'` (V009) + `ChatService.estadoDaSala` + campo `modoTeste` em `GET .../chat/estado` + banner âmbar na `SalaChat.tsx`; `false` restaura a janela (ADR-008) |

## Técnica

> Decididas: **#T1** sessão cookie httpOnly+CSRF (ADR-004) · **#T2** `--release 17` (ADR-001) · **#T3** TypeScript (ADR-002) · **#T4** artefato único + Docker (ADR-009) · **#T6** auto-cadastro com aprovação (ADR-006). **#T5 (SMTP) congelado** — reabrir se a coordenação fornecer servidor institucional (#N9 = sem SMTP por enquanto).
