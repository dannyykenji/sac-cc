-- ADR-005: última linha de defesa contra agendamento duplicado (RNF11/RNF58).
-- SÓ PostgreSQL (H2 não suporta índice parcial; nos testes a garantia é do lock pessimista).
CREATE UNIQUE INDEX uq_agendamento_ativo_por_disponibilidade
    ON agendamento(disponibilidade_id)
    WHERE status IN ('SOLICITADO', 'CONFIRMADO');
