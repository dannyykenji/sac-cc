-- Decisões da coordenação (2026-11-19) — #N1/N5/N7/N9
-- Portável H2↔PG (sem ON CONFLICT; INSERT-SELECT com NOT EXISTS).

-- #N1: confirmação dupla — aluno confirma o comparecimento
ALTER TABLE atendimento ADD COLUMN IF NOT EXISTS confirmado_aluno BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE atendimento ADD COLUMN IF NOT EXISTS data_confirmacao_aluno TIMESTAMP;

-- #N5: antecedência mínima para agendar = 2h (era 24h)
UPDATE parametro_sistema SET valor = '2', descricao = 'Antecedência mínima para agendar em horas (#N5 — coordenação 2026-11-19)'
WHERE chave = 'ANTECEDENCIA_MINIMA_HORAS';

-- #N7: local fixo da coordenação para atendimentos presenciais
INSERT INTO parametro_sistema (chave, valor, descricao)
SELECT 'LOCAL_PRESENCIAL_FIXO', 'Coordenação do Curso de Ciência da Computação',
       'Local fixo de atendimento presencial (#N7 — coordenação 2026-11-19)'
WHERE NOT EXISTS (SELECT 1 FROM parametro_sistema WHERE chave = 'LOCAL_PRESENCIAL_FIXO');

-- #N9: lembrete de chat iminente — minutos antes do horário para avisar no sino
INSERT INTO parametro_sistema (chave, valor, descricao)
SELECT 'LEMBRETE_CHAT_MIN_ANTES', '15',
       'Aviso no sino de chat começando em breve (#N9 — coordenação 2026-11-19)'
WHERE NOT EXISTS (SELECT 1 FROM parametro_sistema WHERE chave = 'LEMBRETE_CHAT_MIN_ANTES');
