-- Modo teste do chat (deploy de avaliação do professor): sala ABERTA desde a criação
-- do agendamento, sem janela de horário. Em uso real, CHAT_MODO_TESTE='false' restaura
-- a janela CHAT_ABRE_MIN_ANTES/CHAT_FECHA_MIN_DEPOIS (ADR-008).
INSERT INTO parametro_sistema (chave, valor, descricao)
SELECT 'CHAT_MODO_TESTE', 'true',
       'Modo teste: sala de chat aberta desde a criação (deploy de avaliação); false restaura a janela parametrizada (ADR-008)'
WHERE NOT EXISTS (SELECT 1 FROM parametro_sistema WHERE chave = 'CHAT_MODO_TESTE');
