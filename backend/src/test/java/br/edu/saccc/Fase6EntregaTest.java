package br.edu.saccc;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Fase 6 — entrega: auditoria consultável (RF64) e notificações do próprio usuário (RF56/60). */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class Fase6EntregaTest {

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;

    private static RequestPostProcessor comoAdmin() {
        return user("admin@saccc.br").authorities(List.of(
                new SimpleGrantedAuthority("ROLE_ADMIN"),
                new SimpleGrantedAuthority("GERENCIAR_USUARIOS")));
    }

    private static RequestPostProcessor comoAluno() {
        return user("aluno@saccc.br").authorities(List.of(
                new SimpleGrantedAuthority("ROLE_ALUNO"),
                new SimpleGrantedAuthority("AGENDAR_ATENDIMENTO")));
    }

    @Test
    void adminConsultaTrilhaDeAuditoria() throws Exception {
        mvc.perform(get("/api/admin/auditoria").with(comoAdmin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void coordenadorEAlunoNaoConsultamAuditoria() throws Exception {
        mvc.perform(get("/api/admin/auditoria")
                        .with(user("coordenacao@saccc.br").roles("COORDENADOR")))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/auditoria").with(comoAluno()))
                .andExpect(status().isForbidden());
    }

    @Test
    void alunoVeSomentePropriasNotificacoes() throws Exception {
        Long alunoId = jdbc.queryForObject("select id from usuario where email='aluno@saccc.br'", Long.class);
        Long coordId = jdbc.queryForObject("select id from usuario where email='coordenacao@saccc.br'", Long.class);
        jdbc.update("delete from notificacao");
        jdbc.update("""
                insert into notificacao (destinatario_id, tipo, mensagem, status_envio, data_criacao)
                values (?, 'STATUS', 'sua notificação', 'REGISTRADA', now())
                """, alunoId);
        jdbc.update("""
                insert into notificacao (destinatario_id, tipo, mensagem, status_envio, data_criacao)
                values (?, 'STATUS', 'notificação de outro', 'REGISTRADA', now())
                """, coordId);

        mvc.perform(get("/api/notificacoes").with(comoAluno()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].mensagem").value("sua notificação"));
    }

    @Test
    void auditoriaSemDadosSensiveisNosDetalhes() throws Exception {
        // detalhes de auditoria nunca contêm senha (RNF25) — amostra o campo
        Integer comSenha = jdbc.queryForObject(
                "select count(*) from auditoria where lower(detalhes) like '%senha%'", Integer.class);
        org.assertj.core.api.Assertions.assertThat(comSenha).isZero();
    }
}
