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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.time.temporal.ChronoUnit;
import br.edu.saccc.service.ZoneSaoPaulo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fase 3 — testes-chave (PLANO_DE_EXECUCAO + RNF40):
 * concorrência 2 threads → exatamente 1 sucesso; duplicado impedido; conflito do próprio
 * aluno impedido (RN03); modalidade incompatível rejeitada (RN15); limite #N4; cancelamento #N3.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class Fase3AgendamentoTest extends BaseLimpoTest {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    @Autowired MockMvc mvc;

    // datas futuras garantidamente fora da antecedência mínima (24h)
    private static final LocalDate DATA = LocalDate.now(ZONE).plusDays(7);

    private static RequestPostProcessor comoAluno(String email) {
        return user(email).authorities(List.of(
                new SimpleGrantedAuthority("ROLE_ALUNO"),
                new SimpleGrantedAuthority("AGENDAR_ATENDIMENTO"),
                new SimpleGrantedAuthority("CANCELAR_AGENDAMENTO")));
    }

    private static RequestPostProcessor comoCoordenador() {
        return user("coordenacao@saccc.br").authorities(List.of(
                new SimpleGrantedAuthority("ROLE_COORDENADOR"),
                new SimpleGrantedAuthority("GERENCIAR_DISPONIBILIDADE"),
                new SimpleGrantedAuthority("REGISTRAR_ATENDIMENTO")));
    }

    @BeforeEach
    void limpar() {
        jdbc.update("delete from agendamento");
        jdbc.update("delete from disponibilidade");
        // segundo aluno para testes de concorrência/isolamento (independente do banco de dev)
        if (jdbc.queryForObject("select count(*) from aluno where matricula='2026990001'", Integer.class) == 0) {
            jdbc.update("""
                    insert into usuario (email, senha_hash, nome, perfil_id, status, data_cadastro)
                    values ('bruno2@saccc.br', 'x', 'Bruno Dois',
                            (select id from perfil_acesso where nome='ALUNO'), 'ATIVO', now())
                    """);
            jdbc.update("""
                    insert into aluno (usuario_id, matricula, curso, status_matricula)
                    select id, '2026990001', 'Ciência da Computação', 'ATIVA'
                    from usuario where email='bruno2@saccc.br'
                    """);
        }
    }

    /** Cria slot direto no banco, retornando o id. */
    private Long slot(String horaInicio, String horaFim, String modalidade) {
        jdbc.update("""
                insert into disponibilidade (data, hora_inicio, hora_fim, duracao_minutos, modalidade,
                                             status, criado_por, data_criacao)
                values (?, ?, ?, 30, ?, 'DISPONIVEL',
                        (select id from usuario where email='coordenacao@saccc.br'), now())
                """, DATA, LocalTime.parse(horaInicio), LocalTime.parse(horaFim), modalidade);
        return jdbc.queryForObject(
                "select id from disponibilidade where data=? and hora_inicio=?",
                Long.class, DATA, LocalTime.parse(horaInicio));
    }

    private Long idAluno(String email) {
        return jdbc.queryForObject("select id from usuario where email=?", Long.class, email);
    }

    private ResultActions agendar(RequestPostProcessor como, Long slotId, String modalidade,
                                  String assunto) throws Exception {
        return mvc.perform(post("/api/agendamentos").with(como).with(csrf())
                .contentType("application/json")
                .content("""
                        {"disponibilidadeId":%d,"modalidade":"%s","assunto":"%s",
                         "descricao":"teste"}
                        """.formatted(slotId, modalidade, assunto)));
    }

    // ---------- RNF40/ADR-005: concorrência ----------

    @Test
    void doisAlunosConcorrendoPeloMesmoSlotExatamenteUmVence() throws Exception {
        Long slotId = slot("14:00", "14:30", "ONLINE");
        jdbc.update("update usuario set status='ATIVO' where email in ('aluno@saccc.br','bruno2@saccc.br')");
        Long bruno = jdbc.queryForObject(
                "select u.id from usuario u join aluno a on a.usuario_id=u.id where a.matricula='2026990001'",
                Long.class);
        assertThat(bruno).isNotNull();

        int threads = 2;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch largada = new CountDownLatch(1);
        AtomicInteger sucesso = new AtomicInteger();
        AtomicInteger conflito = new AtomicInteger();

        Runnable r1 = () -> tentarAgendar(comoAluno("aluno@saccc.br"), slotId, largada, sucesso, conflito);
        Runnable r2 = () -> tentarAgendar(comoAluno("bruno2@saccc.br"), slotId, largada, sucesso, conflito);

        Future<?> f1 = pool.submit(r1);
        Future<?> f2 = pool.submit(r2);
        largada.countDown();
        f1.get(20, TimeUnit.SECONDS);
        f2.get(20, TimeUnit.SECONDS);
        pool.shutdown();

        assertThat(sucesso.get()).as("exatamente 1 agendamento deve vencer").isEqualTo(1);
        assertThat(conflito.get()).as("exatamente 1 deve receber conflito").isEqualTo(1);

        // e o slot fica RESERVADO
        String status = jdbc.queryForObject(
                "select status from disponibilidade where id=?", String.class, slotId);
        assertThat(status).isEqualTo("RESERVADO");
    }

    private void tentarAgendar(RequestPostProcessor como, Long slotId, CountDownLatch largada,
                               AtomicInteger sucesso, AtomicInteger conflito) {
        try {
            largada.await();
            ResultActions r = agendar(como, slotId, "ONLINE", "Concorrência");
            int code = r.andReturn().getResponse().getStatus();
            if (code == 201) sucesso.incrementAndGet();
            else if (code == 409) conflito.incrementAndGet();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ---------- regras de negócio ----------

    @Nested
    class Regras {

        @Test
        void agendarGeraProtocoloEReservaOSlot() throws Exception {
            Long slotId = slot("14:00", "14:30", "ONLINE");
            agendar(comoAluno("aluno@saccc.br"), slotId, "ONLINE", "Dúvida em estágio")
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.protocolo").isNotEmpty())
                    .andExpect(jsonPath("$.protocolo").value(org.hamcrest.Matchers.startsWith("AG-")))
                    .andExpect(jsonPath("$.status").value("SOLICITADO"));
            String st = jdbc.queryForObject(
                    "select status from disponibilidade where id=?", String.class, slotId);
            assertThat(st).isEqualTo("RESERVADO"); // RF16
        }

        @Test
        void slotOcupadoE409ComMensagemClara() throws Exception {
            Long slotId = slot("14:00", "14:30", "ONLINE");
            agendar(comoAluno("aluno@saccc.br"), slotId, "ONLINE", "Primeiro")
                    .andExpect(status().isCreated());
            agendar(comoAluno("bruno2@saccc.br"), slotId, "ONLINE", "Segundo")
                    .andExpect(status().isConflict());
        }

        @Test
        void modalidadeIncompativelE400RN15() throws Exception {
            Long slotId = slot("14:00", "14:30", "PRESENCIAL");
            agendar(comoAluno("aluno@saccc.br"), slotId, "ONLINE", "Tentativa online em presencial")
                    .andExpect(status().isBadRequest());
        }

        @Test
        void hibridoAceitaOnline() throws Exception {
            Long slotId = slot("15:00", "15:30", "HIBRIDO");
            agendar(comoAluno("aluno@saccc.br"), slotId, "ONLINE", "Híbrido escolho online")
                    .andExpect(status().isCreated());
        }

        @Test
        void limiteDeAgendamentosFuturosN4() throws Exception {
            Long s1 = slot("14:00", "14:30", "ONLINE");
            Long s2 = slot("15:00", "15:30", "ONLINE");
            agendar(comoAluno("aluno@saccc.br"), s1, "ONLINE", "Primeiro do dia")
                    .andExpect(status().isCreated());
            // default #N4 = 1 futuro ativo
            agendar(comoAluno("aluno@saccc.br"), s2, "ONLINE", "Segundo do dia")
                    .andExpect(status().isConflict());
        }

        @Test
        void sobreposicaoComProprioAgendamentoRN03() throws Exception {
            // com limite=1 o RN03 é difícil de atingir sem manipular parâmetro;
            // aqui limitamos a 2 para expor a sobreposição
            jdbc.update("update parametro_sistema set valor='2' where chave='LIMITE_AGENDAMENTOS_FUTUROS'");
            Long s1 = slot("14:00", "15:00", "ONLINE");
            Long s2 = slot("14:30", "15:30", "ONLINE"); // sobrepõe 14:30–15:00
            agendar(comoAluno("aluno@saccc.br"), s1, "ONLINE", "Original")
                    .andExpect(status().isCreated());
            agendar(comoAluno("aluno@saccc.br"), s2, "ONLINE", "Sobreposto")
                    .andExpect(status().isConflict());
        }

        @Test
        void antecedenciaMinimaN5() throws Exception {
            // #N5 (coordenação 2026-11-19): antecedência mínima agora é 2h.
            // Slot a MENOS de 2h → barrado (400).
            LocalDateTime agora = LocalDateTime.now(ZoneSaoPaulo.ZONE).truncatedTo(ChronoUnit.MINUTES);
            LocalDateTime breve = agora.plusMinutes(90); // 1h30 à frente < 2h
            jdbc.update("""
                    insert into disponibilidade (data, hora_inicio, hora_fim, duracao_minutos, modalidade,
                                                 status, criado_por, data_criacao)
                    values (?, ?, ?, 30, 'ONLINE', 'DISPONIVEL',
                            (select id from usuario where email='coordenacao@saccc.br'), now())
                    """, breve.toLocalDate(), breve.toLocalTime(), breve.toLocalTime().plusMinutes(30));
            Long id = jdbc.queryForObject(
                    "select id from disponibilidade where data=? and hora_inicio=?",
                    Long.class, breve.toLocalDate(), breve.toLocalTime());
            agendar(comoAluno("aluno@saccc.br"), id, "ONLINE", "Urgente")
                    .andExpect(status().isBadRequest());
        }

        @Test
        void agendamentoComMaisDeDuasHorasEPermitido() throws Exception {
            // #N5: slot a MAIS de 2h → permitido (201)
            LocalDateTime agora = LocalDateTime.now(ZoneSaoPaulo.ZONE).truncatedTo(ChronoUnit.MINUTES);
            LocalDateTime ok = agora.plusHours(2).plusMinutes(30);
            jdbc.update("""
                    insert into disponibilidade (data, hora_inicio, hora_fim, duracao_minutos, modalidade,
                                                 status, criado_por, data_criacao)
                    values (?, ?, ?, 30, 'ONLINE', 'DISPONIVEL',
                            (select id from usuario where email='coordenacao@saccc.br'), now())
                    """, ok.toLocalDate(), ok.toLocalTime(), ok.toLocalTime().plusMinutes(30));
            Long id = jdbc.queryForObject(
                    "select id from disponibilidade where data=? and hora_inicio=?",
                    Long.class, ok.toLocalDate(), ok.toLocalTime());
            agendar(comoAluno("aluno@saccc.br"), id, "ONLINE", "Com antecedência ok")
                    .andExpect(status().isCreated());
        }

        @Test
        void assuntoObrigatorioRF19() throws Exception {
            Long slotId = slot("14:00", "14:30", "ONLINE");
            mvc.perform(post("/api/agendamentos").with(comoAluno("aluno@saccc.br")).with(csrf())
                            .contentType("application/json")
                            .content("""
                                    {"disponibilidadeId":%d,"modalidade":"ONLINE","assunto":"  "}
                                    """.formatted(slotId)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ---------- cancelamento (UC05) ----------

    @Nested
    class Cancelamento {

        @Test
        void alunoCancelaDentroDoPrazoESlotVoltaADisponivel() throws Exception {
            Long slotId = slot("14:00", "14:30", "ONLINE");
            String resp = agendar(comoAluno("aluno@saccc.br"), slotId, "ONLINE", "Vou cancelar")
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Long agId = mapper.readTree(resp).get("id").asLong();

            mvc.perform(post("/api/agendamentos/%d/cancelar".formatted(agId))
                            .with(comoAluno("aluno@saccc.br")).with(csrf())
                            .contentType("application/json")
                            .content("{\"motivo\":\"Conflito de horários na faculdade\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CANCELADO"));

            String st = jdbc.queryForObject(
                    "select status from disponibilidade where id=?", String.class, slotId);
            assertThat(st).isEqualTo("DISPONIVEL"); // slot liberado
        }

        @Test
        void cancelarSemMotivoE400() throws Exception {
            Long slotId = slot("15:00", "15:30", "ONLINE");
            String resp = agendar(comoAluno("aluno@saccc.br"), slotId, "ONLINE", "Sem motivo")
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();
            Long agId = new com.fasterxml.jackson.databind.ObjectMapper().readTree(resp).get("id").asLong();
            mvc.perform(post("/api/agendamentos/%d/cancelar".formatted(agId))
                            .with(comoAluno("aluno@saccc.br")).with(csrf())
                            .contentType("application/json").content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void coordenadorCancelaForaDoPrazoDoAlunoComMotivo() throws Exception {
            // slot para daqui a 2h: aluno não poderia (prazo 24h), coordenação sim
            LocalDateTime breve = slotBreveMenosDe24h();
            jdbc.update("""
                    insert into disponibilidade (data, hora_inicio, hora_fim, duracao_minutos, modalidade,
                                                 status, criado_por, data_criacao)
                    values (?, ?, ?, 30, 'ONLINE', 'DISPONIVEL',
                            (select id from usuario where email='coordenacao@saccc.br'), now())
                    """, breve.toLocalDate(), breve.toLocalTime(), breve.toLocalTime().plusMinutes(30));
            Long slotId = jdbc.queryForObject(
                    "select id from disponibilidade where data=? and hora_inicio=?",
                    Long.class, breve.toLocalDate(), breve.toLocalTime());

            // antecedência atrapalharia o agendamento — zera só para este teste
            jdbc.update("update parametro_sistema set valor='0' where chave='ANTECEDENCIA_MINIMA_HORAS'");
            String resp = agendar(comoAluno("aluno@saccc.br"), slotId, "ONLINE", "Cancelar pela coordenação")
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();
            Long agId = new com.fasterxml.jackson.databind.ObjectMapper().readTree(resp).get("id").asLong();
            jdbc.update("update parametro_sistema set valor='2' where chave='ANTECEDENCIA_MINIMA_HORAS'");  // #N5: novo default

            // aluno tenta e falha (fora do prazo)
            mvc.perform(post("/api/agendamentos/%d/cancelar".formatted(agId))
                            .with(comoAluno("aluno@saccc.br")).with(csrf())
                            .contentType("application/json")
                            .content("{\"motivo\":\"minha tentativa\"}"))
                    .andExpect(status().isConflict());

            // coordenação consegue com motivo
            mvc.perform(post("/api/agendamentos/%d/cancelar".formatted(agId))
                            .with(comoCoordenador()).with(csrf())
                            .contentType("application/json")
                            .content("{\"motivo\":\"Impedimento da coordenação\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CANCELADO"));
        }

        @Test
        void canceladoNaoCancelaDeNovo() throws Exception {
            Long slotId = slot("16:00", "16:30", "ONLINE");
            String resp = agendar(comoAluno("aluno@saccc.br"), slotId, "ONLINE", "Duplo cancelamento")
                    .andExpect(status().isCreated())
                    .andReturn().getResponse().getContentAsString();
            Long agId = new com.fasterxml.jackson.databind.ObjectMapper().readTree(resp).get("id").asLong();
            mvc.perform(post("/api/agendamentos/%d/cancelar".formatted(agId))
                            .with(comoAluno("aluno@saccc.br")).with(csrf())
                            .contentType("application/json")
                            .content("{\"motivo\":\"primeiro\"}"))
                    .andExpect(status().isOk());
            mvc.perform(post("/api/agendamentos/%d/cancelar".formatted(agId))
                            .with(comoAluno("aluno@saccc.br")).with(csrf())
                            .contentType("application/json")
                            .content("{\"motivo\":\"segundo\"}"))
                    .andExpect(status().isConflict());
        }
    }

    // ---------- consulta e isolamento (UC04, RNF18) ----------

    @Nested
    class Consulta {

        @Test
        void alunoVeSomenteSeusAgendamentos() throws Exception {
            Long slotId = slot("14:00", "14:30", "ONLINE");
            agendar(comoAluno("aluno@saccc.br"), slotId, "ONLINE", "Meu")
                    .andExpect(status().isCreated());

            mvc.perform(get("/api/agendamentos/proximos").with(comoAluno("aluno@saccc.br")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].assunto").value("Meu"));

            // detalhe de agendamento de outro aluno → 404 neutro
            Long agId = jdbc.queryForObject("select id from agendamento", Long.class);
            mvc.perform(get("/api/agendamentos/%d".formatted(agId))
                            .with(comoAluno("bruno2@saccc.br")))
                    .andExpect(status().isNotFound());
        }

        @Test
        void coordenadorNaoUsaEndpointsDeAluno() throws Exception {
            mvc.perform(get("/api/agendamentos/proximos").with(comoCoordenador()))
                    .andExpect(status().isForbidden());
        }
    }

    /**
     * Slot futuro a MENOS de 24h (para testar antecedência mínima/prazo de cancelamento)
     * que NUNCA cruza a meia-noite — +2h direto violava o CHECK hora_fim > hora_inicio
     * quando o teste rodava após as 22h (vira 00:xx do dia seguinte).
     */
    /**
     * Slot futuro a MENOS de 24h (antecedência/prazo) que nunca cruza meia-noite:
     * amanhã 10:00–10:30. "Agora + 2h" quebrava o CHECK hora_fim > hora_inicio
     * quando rodava após ~21:30 (fim caía no dia seguinte, 00:xx < início).
     * Amanhã 10h só NÃO atende se agora for após 10h de amanhã — impossível.
     */
    private static LocalDateTime slotBreveMenosDe24h() {
        return LocalDate.now(ZONE).plusDays(1).atTime(10, 0);
    }
}
