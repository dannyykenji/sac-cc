package br.edu.saccc;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fase 1 — testes-chave do plano (PLANO_DE_EXECUCAO.md):
 * acesso por perfil; inativo e pendente bloqueados; duplicidade rejeitada no
 * auto-cadastro (com resposta neutra); senha hasheada; permissões negadas.
 *
 * CSRF: usa o fluxo REAL da SPA — GET /api/auth/csrf entrega o cookie
 * XSRF-TOKEN, que volta como header X-XSRF-TOKEN no POST (ADR-004).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class Fase1AutenticacaoEUsuariosTest extends BaseLimpoTest {

    @Autowired MockMvc mvc;
    @Autowired PasswordEncoder encoder;
    @Autowired br.edu.saccc.repository.UsuarioRepository usuarios;
    @Autowired br.edu.saccc.repository.AlunoRepository alunos;
    @Autowired br.edu.saccc.repository.AuditoriaRepository auditorias;

    private static final String LOGIN = """
            {"email":"%s","senha":"%s"}""";

    /** Mock de usuário com authorities reais: ROLE + permissões (como o login de verdade). */
    private static RequestPostProcessor comoAdmin() {
        return user("admin@saccc.br").authorities(List.of(
                new SimpleGrantedAuthority("ROLE_ADMIN"),
                new SimpleGrantedAuthority("GERENCIAR_USUARIOS"),
                new SimpleGrantedAuthority("APROVAR_CADASTROS"),
                new SimpleGrantedAuthority("CONSULTAR_ESTATISTICAS")));
    }


    /** POST com CSRF injetado (mesma garantia do fluxo SPA, sem depender do cookie compartilhado). */
    private ResultActions postComCsrf(String url, String body, RequestPostProcessor... extras) throws Exception {
        var req = post(url).with(csrf())
                .contentType("application/json").content(body);
        for (RequestPostProcessor extra : extras) req = req.with(extra);
        return mvc.perform(req);
    }

    // ---------- UC01: login ----------

    @Nested
    class Login {

        @Test
        void adminAutenticadoRecebePerfilEPermissoes() throws Exception {
            postComCsrf("/api/auth/login", LOGIN.formatted("admin@saccc.br", "Admin@1234"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.perfil").value("ADMIN"))
                    .andExpect(jsonPath("$.permissoes").isArray());
        }

        @Test
        void senhaErraRetorna401Generico() throws Exception {
            postComCsrf("/api/auth/login", LOGIN.formatted("admin@saccc.br", "errada1"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.detail").value("E-mail ou senha incorretos."));
        }

        @Test
        void semCsrfTokenE403() throws Exception {
            mvc.perform(post("/api/auth/login")
                            .contentType("application/json")
                            .content(LOGIN.formatted("admin@saccc.br", "Admin@1234")))
                    .andExpect(status().isForbidden());
        }

        @Test
        void loginEFalhasGeramAuditoria() throws Exception {
            postComCsrf("/api/auth/login", LOGIN.formatted("admin@saccc.br", "Admin@1234"))
                    .andExpect(status().isOk());
            postComCsrf("/api/auth/login", LOGIN.formatted("admin@saccc.br", "errada1"))
                    .andExpect(status().isUnauthorized());

            var ops = auditorias.findAll().stream().map(a -> a.getOperacao()).toList();
            assertThat(ops).contains("LOGIN", "LOGIN_FALHA");
        }
    }

    // ---------- bloqueio por tentativas (RNF22) ----------

    @Nested
    class BloqueioTentativas {

        @Test
        void aposCincoFalhasBloqueiaCom429() throws Exception {
            for (int i = 0; i < 5; i++) {
                postComCsrf("/api/auth/login", LOGIN.formatted("bloqueio@saccc.br", "errada1"))
                        .andExpect(status().isUnauthorized());
            }
            postComCsrf("/api/auth/login", LOGIN.formatted("bloqueio@saccc.br", "qualquer1"))
                    .andExpect(status().isTooManyRequests());
        }
    }

    // ---------- ADR-006: auto-cadastro ----------

    @Nested
    class AutoCadastro {

        private static final String CORPO = """
                {"nome":"Novo Aluno","email":"novo@saccc.br","matricula":"2026999999",
                 "senha":"Senha1234","periodo":1,"turma":"CC-1A"}""";

        @Test
        void criaContaPendenteComRespostaNeutra() throws Exception {
            postComCsrf("/api/cadastro-aluno", CORPO)
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.mensagem").exists());

            var aluno = alunos.findByStatus(br.edu.saccc.model.StatusUsuario.PENDENTE,
                            PageRequest.of(0, 50))
                    .getContent().stream()
                    .filter(a -> a.getMatricula().equals("2026999999"))
                    .findFirst().orElseThrow();
            assertThat(aluno.getStatus()).isEqualTo(br.edu.saccc.model.StatusUsuario.PENDENTE);

            // senha nunca em texto puro (RNF15)
            assertThat(aluno.getSenhaHash()).startsWith("$2");
            assertThat(encoder.matches("Senha1234", aluno.getSenhaHash())).isTrue();

            // pendente não loga (RF03/UC01)
            postComCsrf("/api/auth/login", LOGIN.formatted("novo@saccc.br", "Senha1234"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void duplicidadeRecebeMesmaRespostaNeutra() throws Exception {
            String corpoDuplicado = """
                    {"nome":"Outro","email":"aluno@saccc.br","matricula":"2026888888",
                     "senha":"Senha1234"}""";
            MvcResult primeiro = postComCsrf("/api/cadastro-aluno", CORPO).andReturn();
            MvcResult duplicado = postComCsrf("/api/cadastro-aluno", corpoDuplicado).andReturn();
            assertThat(duplicado.getResponse().getStatus())
                    .isEqualTo(primeiro.getResponse().getStatus());
            assertThat(alunos.existsByMatricula("2026888888")).isFalse();
        }

        @Test
        void senhaFracaE400() throws Exception {
            postComCsrf("/api/cadastro-aluno",
                    """
                    {"nome":"X","email":"x@saccc.br","matricula":"111","senha":"abc"}""")
                    .andExpect(status().isBadRequest());
        }
    }

    // ---------- ADR-006: aprovação ----------

    @Nested
    class Aprovacao {

        @Test
        void adminAprovaEAlunoPassaALogar() throws Exception {
            postComCsrf("/api/cadastro-aluno", """
                    {"nome":"Ana Aprovada","email":"ana.ap@saccc.br",
                     "matricula":"2026777777","senha":"Senha1234"}""")
                    .andExpect(status().isCreated());

            var pendente = alunos.findByStatus(br.edu.saccc.model.StatusUsuario.PENDENTE,
                            PageRequest.of(0, 50))
                    .getContent().stream()
                    .filter(a -> a.getMatricula().equals("2026777777"))
                    .findFirst().orElseThrow();

            postComCsrf("/api/admin/cadastros-pendentes/%d/aprovar".formatted(pendente.getId()),
                    "{}", comoAdmin())
                    .andExpect(status().isNoContent());

            // aprovar de novo → conflito
            postComCsrf("/api/admin/cadastros-pendentes/%d/aprovar".formatted(pendente.getId()),
                    "{}", comoAdmin())
                    .andExpect(status().isConflict());

            postComCsrf("/api/auth/login", LOGIN.formatted("ana.ap@saccc.br", "Senha1234"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.perfil").value("ALUNO"));
        }

        @Test
        void rejeicaoSemMotivoE400() throws Exception {
            postComCsrf("/api/admin/cadastros-pendentes/999999/rejeitar", "{}", comoAdmin())
                    .andExpect(status().isBadRequest());
        }
    }

    // ---------- UC11: gestão de usuários ----------

    @Nested
    class GestaoUsuarios {

        @Test
        void coordenadorNaoGerenciaUsuarios() throws Exception {
            mvc.perform(get("/api/usuarios")
                            .with(user("coordenacao@saccc.br").roles("COORDENADOR")))
                    .andExpect(status().isForbidden());
        }

        @Test
        void alunoNaoVeCadastrosPendentes() throws Exception {
            mvc.perform(get("/api/admin/cadastros-pendentes")
                            .with(user("aluno@saccc.br").roles("ALUNO")))
                    .andExpect(status().isForbidden());
        }

        @Test
        void ultimoAdminAtivoNaoPodeSerDesativado() throws Exception {
            var admin = usuarios.findByEmailIgnoreCase("admin@saccc.br").orElseThrow();
            postComCsrf("/api/usuarios/%d/desativar".formatted(admin.getId()), "{}", comoAdmin())
                    .andExpect(status().isConflict());
        }

        @Test
        void criarUsuarioComEmailDuplicadoE409() throws Exception {
            postComCsrf("/api/usuarios", """
                    {"nome":"Dup","email":"aluno@saccc.br","perfil":"ALUNO",
                     "senha":"Senha1234","extras":{"matricula":"2026000001"}}""", comoAdmin())
                    .andExpect(status().isConflict());
        }

        @Test
        void desativarImpedeLoginImediatamente() throws Exception {
            postComCsrf("/api/usuarios", """
                    {"nome":"Temp","email":"temp@saccc.br","perfil":"ALUNO",
                     "senha":"Senha1234","extras":{"matricula":"2026555555"}}""", comoAdmin())
                    .andExpect(status().isCreated());

            var temp = usuarios.findByEmailIgnoreCase("temp@saccc.br").orElseThrow();
            postComCsrf("/api/usuarios/%d/desativar".formatted(temp.getId()), "{}", comoAdmin())
                    .andExpect(status().isNoContent());

            postComCsrf("/api/auth/login", LOGIN.formatted("temp@saccc.br", "Senha1234"))
                    .andExpect(status().isForbidden());
        }

        @Test
        void meRetornaUsuarioDaSessao() throws Exception {
            mvc.perform(get("/api/auth/me")
                            .with(user("aluno@saccc.br").roles("ALUNO")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.email").value("aluno@saccc.br"))
                    .andExpect(jsonPath("$.perfil").value("ALUNO"));
        }
    }
}
