package br.edu.saccc;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Base de teste: limpeza PADRÃO entre todos os testes, na ordem correta de FKs.
 * Suítes que precisam de mais limpeza sobrescrevem chamando super.
 * Resolve poluição inter-suítes (surefire roda em ordem alfabética: ChatTest primeiro).
 */
public abstract class BaseLimpoTest {

    @Autowired
    protected JdbcTemplate jdbc;

    @BeforeEach
    void limparBase() {
        // ordem FK: chat → histórico/notificações → atendimentos → agendamentos → disponibilidade
        jdbc.update("delete from mensagem_chat");
        jdbc.update("delete from historico_atendimento");
        jdbc.update("delete from notificacao");
        jdbc.update("delete from atendimento");
        jdbc.update("delete from agendamento");
        jdbc.update("delete from disponibilidade");
        // filhos ANTES dos pais (FK): aluno antes de usuario
        jdbc.update("delete from aluno where matricula in (" +
                "'2026999999','2026888888','2026777777','2026555555','2026000001','2026111111','111')");
        jdbc.update("delete from usuario where email in (" +
                "'temp@saccc.br','x@saccc.br','novo@saccc.br','bruno.teste@saccc.br','ana.ap@saccc.br')");
    }
}
