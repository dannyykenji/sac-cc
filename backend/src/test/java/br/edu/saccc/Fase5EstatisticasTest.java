package br.edu.saccc;

import org.junit.jupiter.api.BeforeEach;
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
import java.time.ZoneId;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fase 5 — testes obrigatórios de indicadores (RNF41, RELATORIOS_ESTATISTICAS.md §5):
 * 1. massa conhecida → resumo exato; 2. recálculo pós-mudança (RNF61);
 * 3. cancelado/ausência fora do % (RN10/12); 4. filtros combinados;
 * 5. período sem dados → zerado; 6. aluno → 403.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class Fase5EstatisticasTest extends BaseLimpoTest {

    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");

    @Autowired MockMvc mvc;

    private static RequestPostProcessor comoCoordenador() {
        return user("coordenacao@saccc.br").authorities(List.of(
                new SimpleGrantedAuthority("ROLE_COORDENADOR"),
                new SimpleGrantedAuthority("CONSULTAR_ESTATISTICAS"),
                new SimpleGrantedAuthority("REGISTRAR_ATENDIMENTO")));
    }

    private static RequestPostProcessor comoAluno() {
        return user("aluno@saccc.br").authorities(List.of(
                new SimpleGrantedAuthority("ROLE_ALUNO"),
                new SimpleGrantedAuthority("AGENDAR_ATENDIMENTO")));
    }

    @BeforeEach
    void limparEMassa() {
        jdbc.update("delete from historico_atendimento");
        jdbc.update("delete from notificacao");
        jdbc.update("delete from atendimento");
        jdbc.update("delete from agendamento");
        jdbc.update("delete from disponibilidade");
        criarMassa();
    }

    private int seq = 0;

    /** Cria um agendamento já atendido (ou ausente/cancelado) com status de resolução. */
    private void evento(String tipo, String modalidade, String categoria, LocalDate data) {
        seq++;
        String ause = "AUSENCIA".equals(tipo) ? "1" : "0";
        String agStatus = switch (tipo) {
            case "AUSENCIA" -> "NAO_COMPARECEU";
            case "CANCELADO" -> "CANCELADO";
            default -> "REALIZADO";
        };
        String slotStatus = "CANCELADO".equals(tipo) ? "DISPONIVEL" : "RESERVADO";
        jdbc.update("""
                insert into disponibilidade (data, hora_inicio, hora_fim, duracao_minutos, modalidade,
                                             status, criado_por, data_criacao)
                values (?, '14:00', '14:30', 30, ?, ?,
                        (select id from usuario where email='coordenacao@saccc.br'), now())
                """, data.toString(), modalidade, slotStatus);
        Long slot = jdbc.queryForObject("select max(id) from disponibilidade", Long.class);
        jdbc.update("""
                insert into agendamento (protocolo, aluno_id, disponibilidade_id, categoria_id, modalidade,
                                         assunto, status, data, hora_inicio, hora_fim, data_criacao)
                values (?, (select id from usuario where email='aluno@saccc.br'), ?, 
                        (select id from categoria_atendimento where nome = ?), ?, ?, ?, ?, '14:00', '14:30', now())
                """, "AG-S5-" + String.format("%04d", seq), slot, categoria, modalidade,
                "evento " + seq, agStatus, data.toString());
        Long ag = jdbc.queryForObject("select max(id) from agendamento", Long.class);
        if (!"CANCELADO".equals(tipo)) {
            String resStatus = "AUSENCIA".equals(tipo) ? "AUSENCIA" : tipo;
            jdbc.update("""
                    insert into atendimento (agendamento_id, responsavel_id, realizado, data_realizacao, status_resolucao, confirmado_aluno)
                    values (?, (select id from usuario where email='coordenacao@saccc.br'), ?, now(), ?, TRUE)
                    """, ag, ause.equals("1") ? false : true, resStatus);
        }
    }

    private void criarMassa() {
        LocalDate hoje = LocalDate.now(ZONE);
        LocalDate esteMes = hoje.withDayOfMonth(5);
        LocalDate mesPassado = hoje.minusMonths(1).withDayOfMonth(10);
        // massa conhecida (conta de mesa): 10 realizados (6R+2NR+2T), 3 cancelados, 2 ausências
        for (int i = 0; i < 6; i++) evento("RESOLVIDO", i % 2 == 0 ? "ONLINE" : "PRESENCIAL", "Estágio", esteMes);
        evento("NAO_RESOLVIDO", "ONLINE", "Disciplinas", esteMes);
        evento("NAO_RESOLVIDO", "PRESENCIAL", "TCC", mesPassado);
        evento("EM_TRATATIVA", "ONLINE", "Estágio", esteMes);
        evento("EM_TRATATIVA", "HIBRIDO", "Outros", mesPassado);
        for (int i = 0; i < 3; i++) evento("CANCELADO", "ONLINE", "Outros", esteMes);
        evento("AUSENCIA", "PRESENCIAL", "Avaliação", esteMes);
        evento("AUSENCIA", "ONLINE", "Outros", mesPassado);
    }

    // ---------- 1: massa conhecida, resumo exato ----------

    @Test
    void resumoBateComAContaDeMesa() throws Exception {
        // sem filtro de período: 10 realizados, 6 resolvidos, 2 não resolvidos, 2 tratativa,
        // 3 cancelados, 2 ausências; % = 6/10 = 60.0
        mvc.perform(get("/api/estatisticas/resumo").with(comoCoordenador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRealizados").value(10))
                .andExpect(jsonPath("$.totalResolvidos").value(6))
                .andExpect(jsonPath("$.totalNaoResolvidos").value(2))
                .andExpect(jsonPath("$.totalEmTratativa").value(2))
                .andExpect(jsonPath("$.totalCancelamentos").value(3))
                .andExpect(jsonPath("$.totalAusencias").value(2))
                .andExpect(jsonPath("$.percentualResolucao").value(60.0));
    }

    // ---------- 3: cancelado/ausência fora do % ----------

    @Test
    void canceladosEAusenciasNaoEntamNoDenominador() throws Exception {
        // % continua 60.0 (6/10) mesmo com 3 cancelados + 2 ausências no período
        mvc.perform(get("/api/estatisticas/resumo").with(comoCoordenador()))
                .andExpect(jsonPath("$.percentualResolucao").value(60.0))
                .andExpect(jsonPath("$.totalRealizados").value(10)); // 10 ≠ 15
    }

    // ---------- 4: filtros combinados ----------

    @Test
    void filtrosCombinadosFiltram() throws Exception {
        LocalDate inicio = LocalDate.now(ZONE).withDayOfMonth(1);
        // só este mês + ONLINE: realizados = 6R/2-este-mês... conta: este mês ONLINE:
        // R:3(i=0,2,4) + NR:1 + T:1 = 5 realizados; resolvidos 3 → 60.0
        mvc.perform(get("/api/estatisticas/resumo").with(comoCoordenador())
                        .param("de", inicio.toString())
                        .param("modalidade", "ONLINE"))
                .andExpect(jsonPath("$.totalRealizados").value(5))
                .andExpect(jsonPath("$.totalResolvidos").value(3))
                .andExpect(jsonPath("$.percentualResolucao").value(60.0));
    }

    @Test
    void distribuicaoPorModalidadeECategoria() throws Exception {
        mvc.perform(get("/api/estatisticas/distribuicao").with(comoCoordenador())
                        .param("dimensao", "MODALIDADE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
        mvc.perform(get("/api/estatisticas/distribuicao").with(comoCoordenador())
                        .param("dimensao", "CATEGORIA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].chave").value("Estágio"))
                .andExpect(jsonPath("$[0].total").value(7));
    }

    @Test
    void evolucaoAgrupaPorMes() throws Exception {
        mvc.perform(get("/api/estatisticas/evolucao").with(comoCoordenador()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    // ---------- 2: recálculo pós-mudança (RNF61) ----------

    @Test
    void indicadorRefleteMudancaDeStatusSemReiniciar() throws Exception {
        // pega um EM_TRATATIVA e resolve via API
        Long agId = jdbc.queryForObject(
                "select agendamento_id from atendimento where status_resolucao='EM_TRATATIVA' limit 1", Long.class);
        mvc.perform(post("/api/atendimentos/%d/status".formatted(agId))
                        .with(comoCoordenador()).with(csrf())
                        .contentType("application/json")
                        .content("""
                                {"statusResolucao":"RESOLVIDO","solucao":"resolvido depois"}"""))
                .andExpect(status().isOk());

        mvc.perform(get("/api/estatisticas/resumo").with(comoCoordenador()))
                .andExpect(jsonPath("$.totalResolvidos").value(7))
                .andExpect(jsonPath("$.totalEmTratativa").value(1))
                .andExpect(jsonPath("$.percentualResolucao").value(70.0)); // 7/10
    }

    // ---------- 5: período sem dados ----------

    @Test
    void periodoSemDadosRetornaZerado() throws Exception {
        mvc.perform(get("/api/estatisticas/resumo").with(comoCoordenador())
                        .param("de", "2020-01-01").param("ate", "2020-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRealizados").value(0))
                .andExpect(jsonPath("$.percentualResolucao").value(0.0));
    }

    // ---------- 6: aluno não acessa ----------

    @Test
    void alunoNaoAcessaEstatisticas() throws Exception {
        mvc.perform(get("/api/estatisticas/resumo").with(comoAluno()))
                .andExpect(status().isForbidden());
    }
}
