package br.edu.saccc;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import br.edu.saccc.service.ZoneSaoPaulo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDate;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fase 2 — testes-chave (PLANO_DE_EXECUCAO):
 * horário conflitante rejeitado (RF13); data passada rejeitada;
 * bloqueado não aparece ao aluno (RF15); local obrigatório no presencial;
 * divisão do lote em slots; permissões.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class Fase2DisponibilidadeTest extends BaseLimpoTest {

    @Autowired MockMvc mvc;

    private static final LocalDate AMANHA = LocalDate.now(java.time.ZoneId.of("America/Sao_Paulo")).plusDays(1);

    private static RequestPostProcessor comoCoordenador() {
        return user("coordenacao@saccc.br").authorities(List.of(
                new SimpleGrantedAuthority("ROLE_COORDENADOR"),
                new SimpleGrantedAuthority("GERENCIAR_DISPONIBILIDADE"),
                new SimpleGrantedAuthority("REGISTRAR_ATENDIMENTO")));
    }

    private static RequestPostProcessor comoAluno() {
        return user("aluno@saccc.br").authorities(List.of(
                new SimpleGrantedAuthority("ROLE_ALUNO"),
                new SimpleGrantedAuthority("AGENDAR_ATENDIMENTO")));
    }

    private ResultActions criarLote(String corpoJson, RequestPostProcessor como) throws Exception {
        return mvc.perform(post("/api/disponibilidades").with(como).with(csrf())
                .contentType("application/json").content(corpoJson));
    }

    private String lote(String inicio, String fim, int duracao, String modalidade, String local) {
        String campoLocal = (local == null) ? "" : local;
        return """
                {"data":"%s","horaInicio":"%s","horaFim":"%s","duracaoMinutos":%d,
                 "modalidade":"%s","local":"%s"}
                """.formatted(AMANHA, inicio, fim, duracao, modalidade, campoLocal);
    }

    // ---------- criação e divisão em slots ----------

    @Nested
    class Criacao {

        @Test
        void loteDeDuasHorasComTrintaMinutosGeraQuatroSlots() throws Exception {
            criarLote(lote("14:00", "16:00", 30, "PRESENCIAL", "Sala 101"), comoCoordenador())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.length()").value(4))
                    .andExpect(jsonPath("$[0].horaInicio").value("14:00"))
                    .andExpect(jsonPath("$[3].horaInicio").value("15:30"))
                    .andExpect(jsonPath("$[0].status").value("DISPONIVEL"));
        }

        @Test
        void duracaoInvalidaE400() throws Exception {
            criarLote(lote("14:00", "15:00", 5, "PRESENCIAL", "Sala 101"), comoCoordenador())
                    .andExpect(status().isBadRequest());
        }

        @Test
        void presencialSemLocalUsaOFixoN7() throws Exception {
            // #N7: sem local informado, o sistema aplica o local fixo da coordenação
            var r = criarLote(lote("14:00", "15:00", 30, "PRESENCIAL", null), comoCoordenador())
                    .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
            org.assertj.core.api.Assertions.assertThat(r).contains("Coordenação do Curso");
        }

        @Test
        void dataPassadaE400() throws Exception {
            String corpo = """
                    {"data":"2020-01-01","horaInicio":"14:00","horaFim":"15:00",
                     "duracaoMinutos":30,"modalidade":"ONLINE"}
                    """;
            criarLote(corpo, comoCoordenador()).andExpect(status().isBadRequest());
        }

        @Test
        void horaInicialJaPassadaHojeE400() throws Exception {
            // mesmo dia, hora de início no passado (ex.: eram 16h e tentou abrir das 14h)
            String ontem = java.time.LocalDate.now(ZoneSaoPaulo.ZONE).toString();
            java.time.LocalTime passado = java.time.LocalTime.now(ZoneSaoPaulo.ZONE).minusMinutes(90);
            String corpo = """
                    {"data":"%s","horaInicio":"%s","horaFim":"%s",
                     "duracaoMinutos":30,"modalidade":"ONLINE"}
                    """.formatted(ontem, passado, passado.plusMinutes(60));
            criarLote(corpo, comoCoordenador()).andExpect(status().isBadRequest());
        }
    }

    // ---------- RF13: conflito ----------

    @Nested
    class Conflito {

        @Test
        void horarioSobrepostoE409() throws Exception {
            criarLote(lote("14:00", "16:00", 30, "ONLINE", null), comoCoordenador())
                    .andExpect(status().isCreated());
            // sobreposição parcial: 15:00–17:00 colide com 15:00–15:30
            criarLote(lote("15:00", "17:00", 30, "ONLINE", null), comoCoordenador())
                    .andExpect(status().isConflict());
        }

        @Test
        void horarioAdjacenteSemSobreposicaoE201() throws Exception {
            criarLote(lote("14:00", "15:00", 30, "ONLINE", null), comoCoordenador())
                    .andExpect(status().isCreated());
            // começa exatamente quando o outro termina → sem conflito
            criarLote(lote("15:00", "16:00", 30, "ONLINE", null), comoCoordenador())
                    .andExpect(status().isCreated());
        }
    }

    // ---------- bloqueio (RF12) e visão do aluno (RF15) ----------

    @Nested
    class BloqueioEVisao {

        @Test
        void bloqueadoNaoApareceParaAlunoMasApareceNoAdmin() throws Exception {
            criarLote(lote("14:00", "15:00", 30, "ONLINE", null), comoCoordenador())
                    .andExpect(status().isCreated());
            Long id = extrairPrimeiroId();

            // aluno vê 2 slots antes do bloqueio
            mvc.perform(get("/api/disponibilidades/publicas").with(comoAluno()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2));

            mvc.perform(post("/api/disponibilidades/%d/bloquear".formatted(id)).with(comoCoordenador()).with(csrf()))
                    .andExpect(status().isNoContent());

            // aluno vê 1 depois; admin continua vendo os 2
            mvc.perform(get("/api/disponibilidades/publicas").with(comoAluno()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
            mvc.perform(get("/api/disponibilidades").with(comoCoordenador()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(2));
        }

        @Test
        void bloquearHorarioReservadoE409() throws Exception {
            criarLote(lote("16:00", "17:00", 30, "ONLINE", null), comoCoordenador())
                    .andExpect(status().isCreated());
            Long id = extrairPrimeiroId("16:00");
            // simula reserva direto no banco (agendamento real chega na Fase 3)
            jdbcTemplateUpdate("update disponibilidade set status='RESERVADO' where id=" + id);
            mvc.perform(post("/api/disponibilidades/%d/bloquear".formatted(id)).with(comoCoordenador()).with(csrf()))
                    .andExpect(status().isConflict());
        }

        private Long extrairPrimeiroId() throws Exception {
            return extrairPrimeiroId("14:00");
        }

        private Long extrairPrimeiroId(String hora) throws Exception {
            String json = mvc.perform(get("/api/disponibilidades").with(comoCoordenador()))
                    .andReturn().getResponse().getContentAsString();
            return extrairIdPorHora(json, hora);
        }
    }

    // ---------- permissões ----------

    @Nested
    class Permissoes {

        @Test
        void alunoNaoCriaDisponibilidade() throws Exception {
            criarLote(lote("14:00", "15:00", 30, "ONLINE", null), comoAluno())
                    .andExpect(status().isForbidden());
        }

        @Test
        void adminSemGerenciarDisponibilidadeE403() throws Exception {
            // ADMIN tem a permissão via V002; aluno não; coordenador sim. Teste negativo com aluno acima.
            // Aqui: coordenador SEM a permissão específica (simulado com role only)
            mvc.perform(get("/api/disponibilidades")
                            .with(user("coordenacao@saccc.br").roles("COORDENADOR")))
                    .andExpect(status().isForbidden());
        }
    }


    private void jdbcTemplateUpdate(String sql) {
        jdbc.update(sql);
    }

    private static Long extrairIdPorHora(String json, String hora) {
        // parse simples: encontra o objeto com horaInicio==hora e pega o id
        var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        try {
            for (var node : mapper.readTree(json)) {
                if (hora.equals(node.get("horaInicio").asText())) {
                    return node.get("id").asLong();
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        throw new IllegalStateException("slot não encontrado: " + hora);
    }
}
