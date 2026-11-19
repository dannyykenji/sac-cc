package br.edu.saccc;

import org.junit.jupiter.api.AfterEach;
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
import java.time.LocalDateTime;
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
 * Chat do atendimento (ADR-008): sala por agendamento, janela parametrizada,
 * append-only, isolamento (404 neutro em sala alheia), estados da sala.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ChatTest extends BaseLimpoTest {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    @Autowired MockMvc mvc;

    private static RequestPostProcessor comoAluno() {
        return user("aluno@saccc.br").authorities(List.of(
                new SimpleGrantedAuthority("ROLE_ALUNO"),
                new SimpleGrantedAuthority("AGENDAR_ATENDIMENTO")));
    }

    private static RequestPostProcessor comoOutroAluno() {
        return user("bruno2@saccc.br").authorities(List.of(
                new SimpleGrantedAuthority("ROLE_ALUNO"),
                new SimpleGrantedAuthority("AGENDAR_ATENDIMENTO")));
    }

    private static RequestPostProcessor comoCoordenador() {
        return user("coordenacao@saccc.br").authorities(List.of(
                new SimpleGrantedAuthority("ROLE_COORDENADOR"),
                new SimpleGrantedAuthority("REGISTRAR_ATENDIMENTO")));
    }

    // bruno2 (segundo aluno) é criado sob demanda; a base (BaseLimpoTest) cuida da limpeza padrão
    @BeforeEach
    void garantirBruno2() {
        // CHAT_MODO_TESTE sujo de outra suíte faria TODA sala abrir fora da janela —
        // as suítes desta classe validam a janela real; o modo teste tem @Nested próprio.
        jdbc.update("update parametro_sistema set valor='false' where chave='CHAT_MODO_TESTE'");
        if (jdbc.queryForObject("select count(*) from usuario where email='bruno2@saccc.br'", Integer.class) == 0) {
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

    /** Agendamento ONLINE com horário dentro da janela (agora ± minutos). */
    private Long agendamentoOnlineEm(LocalDateTime quando, String statusAg) {
        jdbc.update("""
                insert into disponibilidade (data, hora_inicio, hora_fim, duracao_minutos, modalidade,
                                             status, criado_por, data_criacao)
                values (?, ?, ?, 30, 'ONLINE', 'RESERVADO',
                        (select id from usuario where email='coordenacao@saccc.br'), now())
                """, java.sql.Date.valueOf(quando.toLocalDate()),
                java.sql.Time.valueOf(quando.toLocalTime()),
                java.sql.Time.valueOf(quando.toLocalTime().plusMinutes(30)));
        Long slot = jdbc.queryForObject("select max(id) from disponibilidade", Long.class);
        String proto = "AG-C-" + System.nanoTime() % 1_000_000;
        jdbc.update("""
                insert into agendamento (protocolo, aluno_id, disponibilidade_id, modalidade, assunto,
                                         status, data, hora_inicio, hora_fim, data_criacao)
                values (?, (select id from usuario where email='aluno@saccc.br'), ?, 'ONLINE', 'chat teste',
                        ?, ?, ?, ?, now())
                """, proto, slot, statusAg, java.sql.Date.valueOf(quando.toLocalDate()),
                java.sql.Time.valueOf(quando.toLocalTime()),
                java.sql.Time.valueOf(quando.toLocalTime().plusMinutes(30)));
        return jdbc.queryForObject("select max(id) from agendamento", Long.class);
    }

    private String corpo(String texto) {
        return "{\"conteudo\":\"" + texto + "\"}";
    }

    @Nested
    class JanelaDaSala {

        @Test
        void estadoAbertaDentroDaJanela() throws Exception {
            Long ag = agendamentoOnlineEm(LocalDateTime.now(ZONE).plusMinutes(5), "SOLICITADO");
            mvc.perform(get("/api/agendamentos/%d/chat/estado".formatted(ag)).with(comoAluno()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.estado").value("ABERTA"));
        }

        @Test
        void estadoFechadaAntesDaJanela() throws Exception {
            Long ag = agendamentoOnlineEm(LocalDateTime.now(ZONE).plusHours(3), "SOLICITADO");
            mvc.perform(get("/api/agendamentos/%d/chat/estado".formatted(ag)).with(comoAluno()))
                    .andExpect(jsonPath("$.estado").value("FECHADA"));
        }

        @Test
        void estadoEncerradaAposAtendimentoRegistrado() throws Exception {
            Long ag = agendamentoOnlineEm(LocalDateTime.now(ZONE).plusMinutes(5), "REALIZADO");
            mvc.perform(get("/api/agendamentos/%d/chat/estado".formatted(ag)).with(comoAluno()))
                    .andExpect(jsonPath("$.estado").value("ENCERRADA"));
        }
    }

    @Nested
    class Envio {

        @Test
        void alunoEnviaRecebe201EVoltaNoHistorico() throws Exception {
            Long ag = agendamentoOnlineEm(LocalDateTime.now(ZONE).plusMinutes(5), "SOLICITADO");
            mvc.perform(post("/api/agendamentos/%d/chat".formatted(ag)).with(comoAluno()).with(csrf())
                            .contentType("application/json").content(corpo("Olá, bom dia!")))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.conteudo").value("Olá, bom dia!"))
                    .andExpect(jsonPath("$.perfilAutor").value("ALUNO"));

            mvc.perform(get("/api/agendamentos/%d/chat".formatted(ag)).with(comoAluno()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }

        @Test
        void coordenadorRespondeNaSala() throws Exception {
            Long ag = agendamentoOnlineEm(LocalDateTime.now(ZONE).plusMinutes(5), "SOLICITADO");
            mvc.perform(post("/api/agendamentos/%d/chat".formatted(ag)).with(comoCoordenador()).with(csrf())
                            .contentType("application/json").content(corpo("Oi! Já te atendo em instantes.")))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.perfilAutor").value("COORDENADOR"));
        }

        @Test
        void salaFechadaRejeitaEnvioCom409() throws Exception {
            Long ag = agendamentoOnlineEm(LocalDateTime.now(ZONE).plusHours(3), "SOLICITADO");
            mvc.perform(post("/api/agendamentos/%d/chat".formatted(ag)).with(comoAluno()).with(csrf())
                            .contentType("application/json").content(corpo("cedado demais")))
                    .andExpect(status().isConflict());
        }

        @Test
        void salaEncerradaSomenteLeitura() throws Exception {
            Long ag = agendamentoOnlineEm(LocalDateTime.now(ZONE).plusMinutes(5), "REALIZADO");
            mvc.perform(post("/api/agendamentos/%d/chat".formatted(ag)).with(comoAluno()).with(csrf())
                            .contentType("application/json").content(corpo("tarde demais")))
                    .andExpect(status().isConflict());
            // mas o histórico continua legível
            mvc.perform(get("/api/agendamentos/%d/chat".formatted(ag)).with(comoAluno()))
                    .andExpect(status().isOk());
        }

        @Test
        void mensagemVaziaE400() throws Exception {
            Long ag = agendamentoOnlineEm(LocalDateTime.now(ZONE).plusMinutes(5), "SOLICITADO");
            mvc.perform(post("/api/agendamentos/%d/chat".formatted(ag)).with(comoAluno()).with(csrf())
                            .contentType("application/json").content(corpo("   ")))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void mensagemAcimaDe2000E400() throws Exception {
            Long ag = agendamentoOnlineEm(LocalDateTime.now(ZONE).plusMinutes(5), "SOLICITADO");
            String grande = "a".repeat(2001);
            mvc.perform(post("/api/agendamentos/%d/chat".formatted(ag)).with(comoAluno()).with(csrf())
                            .contentType("application/json").content(corpo(grande)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class ModoTeste {

        @Test
        void salaAbertaDesdeACriacaoForaDaJanela() throws Exception {
            jdbc.update("update parametro_sistema set valor='true' where chave='CHAT_MODO_TESTE'");
            // agendamento daqui a 3h: fora da janela real, mas modo teste abre desde a criação
            Long ag = agendamentoOnlineEm(LocalDateTime.now(ZONE).plusHours(3), "SOLICITADO");
            mvc.perform(get("/api/agendamentos/%d/chat/estado".formatted(ag)).with(comoAluno()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.estado").value("ABERTA"))
                    .andExpect(jsonPath("$.modoTeste").value(true));
            mvc.perform(post("/api/agendamentos/%d/chat".formatted(ag)).with(comoAluno()).with(csrf())
                            .contentType("application/json").content(corpo("teste no modo avaliação")))
                    .andExpect(status().isCreated());
        }

        @Test
        void encerradaContinuaSomenteLeituraMesmoEmModoTeste() throws Exception {
            jdbc.update("update parametro_sistema set valor='true' where chave='CHAT_MODO_TESTE'");
            Long ag = agendamentoOnlineEm(LocalDateTime.now(ZONE).plusHours(3), "REALIZADO");
            mvc.perform(get("/api/agendamentos/%d/chat/estado".formatted(ag)).with(comoAluno()))
                    .andExpect(jsonPath("$.estado").value("ENCERRADA"))
                    .andExpect(jsonPath("$.modoTeste").value(true));
            mvc.perform(post("/api/agendamentos/%d/chat".formatted(ag)).with(comoAluno()).with(csrf())
                            .contentType("application/json").content(corpo("não")))
                    .andExpect(status().isConflict());
        }

        @AfterEach
        void restaurarModoReal() {
            jdbc.update("update parametro_sistema set valor='false' where chave='CHAT_MODO_TESTE'");
        }
    }

    @Nested
    class Isolamento {

        @Test
        void outroAlunoRecebe404Neutro() throws Exception {
            Long ag = agendamentoOnlineEm(LocalDateTime.now(ZONE).plusMinutes(5), "SOLICITADO");
            mvc.perform(get("/api/agendamentos/%d/chat".formatted(ag)).with(comoOutroAluno()))
                    .andExpect(status().isNotFound());
            mvc.perform(post("/api/agendamentos/%d/chat".formatted(ag)).with(comoOutroAluno()).with(csrf())
                            .contentType("application/json").content(corpo("invasão")))
                    .andExpect(status().isNotFound());
        }

        @Test
        void presencialNaoDeveriaTerChatMasARegraEaMesma() throws Exception {
            // sala é por agendamento; modalidade ONLINE é o caso de uso (ADR-008).
            // aqui validamos apenas que o acesso continua controlado para qualquer agendamento.
            Long ag = agendamentoOnlineEm(LocalDateTime.now(ZONE).plusMinutes(5), "SOLICITADO");
            mvc.perform(get("/api/agendamentos/%d/chat/estado".formatted(ag)).with(comoAluno()))
                    .andExpect(status().isOk());
        }
    }
}
