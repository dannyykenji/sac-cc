-- ADMIN também registra atendimentos: a agenda administrativa (C1) exige
-- REGISTRAR_ATENDIMENTO, e o menu oferece a tela para ADMIN. Sem esta
-- permissão a agenda carregava vazia (403 silencioso no front).
INSERT INTO perfil_permissao (perfil_id, permissao_id)
SELECT p.id, m.id FROM perfil_acesso p, permissao m
WHERE p.nome = 'ADMIN' AND m.codigo = 'REGISTRAR_ATENDIMENTO'
  AND NOT EXISTS (SELECT 1 FROM perfil_permissao pp
                  WHERE pp.perfil_id = p.id AND pp.permissao_id = m.id);
