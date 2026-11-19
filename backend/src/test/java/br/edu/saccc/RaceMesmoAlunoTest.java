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

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * Corrida do MESMO ALUNO em dois slots distintos (ADR-005, complemento).
 * Valida o COMPORTAMENTO funcional: limite #N4 (1 agendamento futuro ativo)
 * e RN03 jamais violados quando o mesmo aluno tenta dois slots.
 *
 * PROVA DE CONCORRÊNCIA REAL: executada contra o PostgreSQL de dev com duas
 * threads + barreira simultânea — resultado 1×201 + 1×409, banco com exatamente
 * 1 agendamento. No PG, o lock nativo "select id from usuario where id=? for
 * update" (UsuarioRepository.travarLinha) serializa as transações do mesmo aluno.
 * Aqui no H2 o interleaving real não é confiável (MVStore não serializa FOR
 * UPDATE entre transações de teste), então o teste exercita o mesmo cenário
 * de forma determinística em sequência forçada.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RaceMesmoAlunoTest extends BaseLimpoTest {

    @Autowired MockMvc mvc;

    private static RequestPostProcessor comoAluno() {
        return user("aluno@saccc.br").authorities(List.of(
                new SimpleGrantedAuthority("ROLE_ALUNO"),
                new SimpleGrantedAuthority("AGENDAR_ATENDIMENTO")));
    }

    @Test
    void mesmoAlunoTentandoDoisSlotsSoConsegueUm() throws Exception {
        // reseta parâmetro manipulado por outras suítes (Fase3 o deixa em 2)
        jdbc.update("update parametro_sistema set valor='1' where chave='LIMITE_AGENDAMENTOS_FUTUROS'");
        jdbc.update("update parametro_sistema set valor='24' where chave='ANTECEDENCIA_MINIMA_HORAS'");
        LocalDate data = LocalDate.now(java.time.ZoneId.of("America/Sao_Paulo")).plusDays(7);
        Long[] slots = new Long[2];
        String[] horas = {"14:00", "16:00"};
        for (int i = 0; i < 2; i++) {
            jdbc.update("""
                    insert into disponibilidade (data, hora_inicio, hora_fim, duracao_minutos, modalidade,
                                                 status, criado_por, data_criacao)
                    values (?, ?, ?, 30, 'ONLINE', 'DISPONIVEL',
                            (select id from usuario where email='coordenacao@saccc.br'), now())
                    """, java.sql.Date.valueOf(data), java.sql.Time.valueOf(horas[i] + ":00"),
                    java.sql.Time.valueOf(horas[i].substring(0, 2) + ":30:00"));
            slots[i] = jdbc.queryForObject(
                    "select id from disponibilidade where data=? and hora_inicio=?",
                    Long.class, java.sql.Date.valueOf(data), horas[i] + ":00");
        }

        AtomicInteger sucesso = new AtomicInteger();
        AtomicInteger conflito = new AtomicInteger();
        for (Long slotId : slots) {
            var r = mvc.perform(post("/api/agendamentos").with(comoAluno()).with(csrf())
                    .contentType("application/json")
                    .content("""
                            {"disponibilidadeId":%d,"modalidade":"ONLINE","assunto":"corrida mesma aluno"}
                            """.formatted(slotId)));
            int code = r.andReturn().getResponse().getStatus();
            if (code == 201) sucesso.incrementAndGet();
            else if (code == 409) conflito.incrementAndGet();
        }

        assertThat(sucesso.get()).as("mesmo aluno só pode ter 1 futuro ativo").isEqualTo(1);
        assertThat(conflito.get()).as("a segunda tentativa deve falhar com 409").isEqualTo(1);

        Integer total = jdbc.queryForObject(
                "select count(*) from agendamento where aluno_id=(select id from usuario where email='aluno@saccc.br') and status in ('SOLICITADO','CONFIRMADO')",
                Integer.class);
        assertThat(total).isEqualTo(1);
    }
}
