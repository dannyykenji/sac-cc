package br.edu.saccc;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
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
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fase 4 — testes-chave (PLANO_DE_EXECUCAO):
 * transições inválidas rejeitadas; histórico preservado após mudança (RN13);
 * RN07/08/09 aplicadas; ausência nunca vira resolvido (RN12); observações internas
 * nunca expostas ao aluno (RF43).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class Fase4AtendimentoTest extends BaseLimpoTest {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    @Autowired MockMvc mvc;

    private static RequestPostProcessor comoCoordenador() {
        return user("coordenacao@saccc.br").authorities(List.of(
                new SimpleGrantedAuthority("ROLE_COORDENADOR"),
                new SimpleGrantedAuthority("REGISTRAR_ATENDIMENTO"),
                new SimpleGrantedAuthority("GERENCIAR_DISPONIBILIDADE")));
    }

    private static RequestPostProcessor comoAluno() {
        return user("aluno@saccc.br").authorities(List.of(
                new SimpleGrantedAuthority("ROLE_ALUNO"),
                new SimpleGrantedAuthority("AGENDAR_ATENDIMENTO")));
    }

    @BeforeEach
    void limpar() {
        jdbc.update("delete from historico_atendimento");
        jdbc.update("delete from notificacao");
        jdbc.update("delete from atendimento");
        jdbc.update("delete from agendamento");
        jdbc.update("delete from disponibilidade");
    }

    /** Cria slot + agendamento ativo (via JDBC) e retorna o id do agendamento. */
    private Long agendamentoAtivo(String assunto) {
        LocalDate data = LocalDate.now(ZONE).plusDays(5);
        jdbc.update("""
                insert into disponibilidade (data, hora_inicio, hora_fim, duracao_minutos, modalidade,
                                             status, criado_por, data_criacao)
                values (?, '14:00', '14:30', 30, 'ONLINE', 'RESERVADO',
                        (select id from usuario where email='coordenacao@saccc.br'), now())
                """, data);
        Long slotId = jdbc.queryForObject("select max(id) from disponibilidade", Long.class);
        // protocolo único por chamada (RN04) — usa nanotime
        String proto = "AG-T-" + String.format("%06d", System.nanoTime() % 1_000_000);
        jdbc.update("""
                insert into agendamento (protocolo, aluno_id, disponibilidade_id, modalidade, assunto,
                                         status, data, hora_inicio, hora_fim, data_criacao)
                values (?, (select id from usuario where email='aluno@saccc.br'), ?, 'ONLINE', ?,
                        'SOLICITADO', ?, '14:00', '14:30', now())
                """, proto, slotId, assunto, data);
        return jdbc.queryForObject("select max(id) from agendamento", Long.class);
    }

    private org.springframework.test.web.servlet.ResultActions registrar(Long agId, String corpo) throws Exception {
        return mvc.perform(post("/api/atendimentos").with(comoCoordenador()).with(csrf())
                .contentType("application/json").content(corpo.formatted(agId)));
    }

    // ---------- RN07/08/09: campos condicionais ----------

    @Nested
    class CamposCondicionais {

        @Test
        void resolvidoSemSolucaoE400() throws Exception {
            Long agId = agendamentoAtivo("Estágio");
            registrar(agId, """
                    {"agendamentoId":%d,"realizado":true,"statusResolucao":"RESOLVIDO",
                     "observacoes":"ok"}""")
                    .andExpect(status().isBadRequest());
        }

        @Test
        void naoResolvidoSemJustificativaE400() throws Exception {
            Long agId = agendamentoAtivo("TCC");
            registrar(agId, """
                    {"agendamentoId":%d,"realizado":true,"statusResolucao":"NAO_RESOLVIDO"}""")
                    .andExpect(status().isBadRequest());
        }

        @Test
        void emTratativaSemPrazoE400() throws Exception {
            Long agId = agendamentoAtivo("Disciplinas");
            registrar(agId, """
                    {"agendamentoId":%d,"realizado":true,"statusResolucao":"EM_TRATATIVA",
                     "observacoes":"aguardando secretaria"}""")
                    .andExpect(status().isBadRequest());
        }

        @Test
        void resolvidoComSolucaoCriaAtendimentoHistoricoENotificacao() throws Exception {
            Long agId = agendamentoAtivo("Aproveitamento");
            registrar(agId, """
                    {"agendamentoId":%d,"realizado":true,"statusResolucao":"RESOLVIDO",
                     "solucao":"Equivalência aprovada e registrada no histórico.",
                     "observacoes":"aluno trouxe ementário"}""")
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.statusResolucao").value("RESOLVIDO"));

            // agendamento virou REALIZADO
            String st = jdbc.queryForObject("select status from agendamento where id=?", String.class, agId);
            org.assertj.core.api.Assertions.assertThat(st).isEqualTo("REALIZADO");
            // histórico criado (RN13)
            Integer h = jdbc.queryForObject("select count(*) from historico_atendimento where atendimento_id=?", Integer.class, agId);
            org.assertj.core.api.Assertions.assertThat(h).isEqualTo(1);
            // notificação registrada (RF56)
            Integer n = jdbc.queryForObject("select count(*) from notificacao where agendamento_id=?", Integer.class, agId);
            org.assertj.core.api.Assertions.assertThat(n).isGreaterThanOrEqualTo(1);
        }
    }

    // ---------- RN12: ausência ----------

    @Nested
    class Ausencia {

        @Test
        void ausenciaSemStatusENaoCompareceu() throws Exception {
            Long agId = agendamentoAtivo("Frequência");
            registrar(agId, """
                    {"agendamentoId":%d,"realizado":false,"observacoes":"aluno não apareceu"}""")
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.realizado").value(false));

            String st = jdbc.queryForObject("select status from agendamento where id=?", String.class, agId);
            org.assertj.core.api.Assertions.assertThat(st).isEqualTo("NAO_COMPARECEU");
            String sr = jdbc.queryForObject("select status_resolucao from atendimento where agendamento_id=?", String.class, agId);
            org.assertj.core.api.Assertions.assertThat(sr).isEqualTo("AUSENCIA"); // nunca RESOLVIDO (RN12)
        }

        @Test
        void ausenciaComStatusE400() throws Exception {
            Long agId = agendamentoAtivo("Avaliação");
            registrar(agId, """
                    {"agendamentoId":%d,"realizado":false,"statusResolucao":"RESOLVIDO","solucao":"x"}""")
                    .andExpect(status().isBadRequest());
        }
    }

    // ---------- UC09: transições e histórico (RN13) ----------

    @Nested
    class Transicoes {

        private Long atendidoEmTratativa() throws Exception {
            Long agId = agendamentoAtivo("Projeto");
            registrar(agId, """
                    {"agendamentoId":%d,"realizado":true,"statusResolucao":"EM_TRATATIVA",
                     "observacoes":"aguardando documento","prazoRetorno":"2027-01-15"}""")
                    .andExpect(status().isCreated());
            return agId;
        }

        @Test
        void tratativaParaResolvidoComSolucaoENotifica() throws Exception {
            Long agId = atendidoEmTratativa();
            mvc.perform(post("/api/atendimentos/%d/status".formatted(agId)).with(comoCoordenador()).with(csrf())
                            .contentType("application/json")
                            .content("""
                                    {"statusResolucao":"RESOLVIDO","solucao":"Documento recebido e validado."}"""))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.statusResolucao").value("RESOLVIDO"));

            // histórico preservado: 2 entradas (registro + transição) — RN13
            Integer h = jdbc.queryForObject("select count(*) from historico_atendimento where atendimento_id=?", Integer.class, agId);
            org.assertj.core.api.Assertions.assertThat(h).isEqualTo(2);
        }

        @Test
        void encerradoSemJustificativaE400RN14() throws Exception {
            Long agId = atendidoEmTratativa();
            mvc.perform(post("/api/atendimentos/%d/status".formatted(agId)).with(comoCoordenador()).with(csrf())
                            .contentType("application/json")
                            .content("""
                                    {"statusResolucao":"RESOLVIDO","solucao":"ok"}"""))
                    .andExpect(status().isOk());
            // agora encerrado → mudar exige justificativa
            mvc.perform(post("/api/atendimentos/%d/status".formatted(agId)).with(comoCoordenador()).with(csrf())
                            .contentType("application/json")
                            .content("""
                                    {"statusResolucao":"NAO_RESOLVIDO","justificativa":""}"""))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void statusInvalidoE400() throws Exception {
            Long agId = atendidoEmTratativa();
            mvc.perform(post("/api/atendimentos/%d/status".formatted(agId)).with(comoCoordenador()).with(csrf())
                            .contentType("application/json")
                            .content("""
                                    {"statusResolucao":"URGENTE"}"""))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void registrarDuasVezesE409() throws Exception {
            Long agId = agendamentoAtivo("Outros");
            registrar(agId, """
                    {"agendamentoId":%d,"realizado":true,"statusResolucao":"RESOLVIDO","solucao":"ok"}""")
                    .andExpect(status().isCreated());
            registrar(agId, """
                    {"agendamentoId":%d,"realizado":true,"statusResolucao":"RESOLVIDO","solucao":"de novo"}""")
                    .andExpect(status().isConflict());
        }
    }

    // ---------- RF43: observações internas ----------

    @Nested
    class Isolamento {

        @Test
        void alunoNaoAcessaAtendimentoNemAgenda() throws Exception {
            Long agId = agendamentoAtivo("Sigiloso");
            registrar(agId, """
                    {"agendamentoId":%d,"realizado":true,"statusResolucao":"RESOLVIDO",
                     "solucao":"ok","observacoes":"OBS INTERNA: aluno instável"}""")
                    .andExpect(status().isCreated());

            mvc.perform(get("/api/atendimentos/%d".formatted(agId)).with(comoAluno()))
                    .andExpect(status().isForbidden());
            mvc.perform(get("/api/admin/agenda").with(comoAluno()))
                    .andExpect(status().isForbidden());
        }

        @Test
        void agendaAdminFiltraPorStatusEPagina() throws Exception {
            agendamentoAtivo("Um");
            agendamentoAtivo("Dois");
            mvc.perform(get("/api/admin/agenda").with(comoCoordenador())
                            .param("status", "SOLICITADO")
                            .param("page", "0").param("size", "1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1))
                    .andExpect(jsonPath("$.totalElements").value(2));
        }
    }

}
