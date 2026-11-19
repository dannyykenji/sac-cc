# ADR-008 — Chat do atendimento online (sala por agendamento, WebSocket+STOMP)

**Status:** ACEITA (decisões do usuário em 2026-11-05)
**Data:** 2026-11-05

## Contexto

- Pós-MVP: o atendimento ONLINE hoje só registra a modalidade (com link opcional de reunião). Não existe medium para a conversa em si.
- Documento de requisitos (1.5) exclui **chatbot** e videoconferência integrada "salvo decisão posterior" — o usuário exerceu a decisão posterior: chat humano aluno↔coordenação.
- Backend estável (66 testes); frontend será refeito — o chat entra como peça de primeira classe no rebuild.

## Decisões (escolhidas pelo usuário)

1. **Sala por agendamento (1:1)**: cada agendamento com modalidade ONLINE (inclui HÍBRIDO escolhido online) possui UMA sala de chat. Participantes: o aluno do agendamento + qualquer usuário com `REGISTRAR_ATENDIMENTO` (inbox compartilhada da coordenação — o aluno não espera "o" coordenador; o responsável formal continua sendo quem registra o atendimento).
2. **WebSocket + STOMP** (`spring-boot-starter-websocket`): destino `/topic/agendamento/{id}` para receber, `POST /api/agendamentos/{id}/chat` para enviar (mensagem via REST → persiste → difunde no tópico). Autorização pela **mesma sessão HTTP** do Spring Security (sem token novo), destino validado no servidor.
3. **Mensagens imutáveis (append-only)**: sem edição/exclusão por qualquer parte — o chat é registro oficial do atendimento, coerente com RN13. Erro de digitação → nova mensagem com correção (como termo de uso de sistemas jurídicos).
4. **Somente texto** na v1 (sem anexos): anexos exigiriam armazenamento, validação de tipo/tamanho e tratamento LGPD adicional — fica como evolução (tabela `anexo_mensagem` 1:N no futuro).

## Ciclo de vida da sala (parametrizável em `parametro_sistema`, como #N3/N4/N5)

| Chave | Default | Efeito |
|---|---|---|
| `CHAT_ABRE_MIN_ANTES` | 10 | sala abre X min antes da hora inicial do agendamento |
| `CHAT_FECHA_MIN_DEPOIS` | 15 | sala fecha X min após a hora final (ou quando o atendimento é registrado) |

Estados: `FECHADA` (fora da janela/atendimento registrado) → `ABERTA` (na janela) → `ENCERRADA` (após atendimento registrado; **somente leitura**, histórico preservado).

### Modo teste do deploy de avaliação (2026-11-19)

O deploy destinado à avaliação (usuário + professor) roda com `CHAT_MODO_TESTE='true'` (V009): a sala fica **ABERTA desde a criação do agendamento**, sem janela de horário, e a UI exibe aviso âmbar explícito de que a abertura contínua vale apenas para o ambiente de teste — em uso real a janela parametrizada se aplica. Atendimento registrado continua ENCERRANDO a sala mesmo em modo teste (o registro é invariante). Em produção, `CHAT_MODO_TESTE='false'` restaura integralmente a janela acima; o parâmetro é lido a cada chamada (sem restart).

## Regras de acesso (servidor, RNF17/18)

- Aluno: entra SOMENTE na sala dos próprios agendamentos (404 neutro nos demais).
- Envio: apenas com sala ABERTA (FECHADA/ENCERRADA → 409 com mensagem clara).
- Leitura do histórico: permitida a qualquer momento pelas partes (mesma regra de acesso) — é parte do registro do atendimento.
- Tamanho da mensagem: 1–2000 caracteres, validado.
- Moderação: nenhuma (medium 1:1 já restrito; auditoria registra entrada na sala e envios).

## Modelo de dados

```sql
CREATE TABLE mensagem_chat (
    id BIGINT IDENTITY PRIMARY KEY,
    agendamento_id BIGINT NOT NULL REFERENCES agendamento(id),   -- sala = agendamento
    autor_id BIGINT NOT NULL REFERENCES usuario(id),
    conteudo VARCHAR(2000) NOT NULL,
    data_envio TIMESTAMP NOT NULL
);
CREATE INDEX idx_chat_agendamento ON mensagem_chat(agendamento_id, data_envio);
```

Sala não é tabela: o agendamento É a sala (1:1 direto); estado deriva da janela parametrizada + status do agendamento. Zero novas tabelas além da de mensagens.

## Consequências

**Positivas**
- Atendimento online ganha o medium que faltava; transição portal→sistema contínua.
- Isolamento reaproveita verificação existente (`pertenceAoAluno`); coordenação é time.
- Histórico do chat audita o que foi combinado — valor jurídico/administrativo.

**Negativas**
- Estado da sala é derivado (janela × parâmetros) — coordenação não força abrir/fechar manualmente na v1 (evolução possível).
- STOMP exige sessão sticky se escalar horizontalmente (irrelevante no deploy institucional único previsto).
- Uma conexão WS por usuário ativo conectado.
