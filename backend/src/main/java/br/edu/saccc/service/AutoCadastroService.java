package br.edu.saccc.service;

import br.edu.saccc.dto.AutoCadastroRequest;
import br.edu.saccc.model.Aluno;
import br.edu.saccc.model.PerfilAcesso;
import br.edu.saccc.model.StatusUsuario;
import br.edu.saccc.repository.AlunoRepository;
import br.edu.saccc.repository.PerfilAcessoRepository;
import br.edu.saccc.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Auto-cadastro público com aprovação (ADR-006): conta nasce PENDENTE;
 * resposta sempre neutra (anti-enumeration, RNF25).
 */
@Service
public class AutoCadastroService {

    private final UsuarioRepository usuarioRepository;
    private final AlunoRepository alunoRepository;
    private final PerfilAcessoRepository perfilRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaService auditoria;

    public AutoCadastroService(UsuarioRepository usuarioRepository,
                               AlunoRepository alunoRepository,
                               PerfilAcessoRepository perfilRepository,
                               PasswordEncoder passwordEncoder,
                               AuditoriaService auditoria) {
        this.usuarioRepository = usuarioRepository;
        this.alunoRepository = alunoRepository;
        this.perfilRepository = perfilRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditoria = auditoria;
    }

    @Transactional
    public void registrar(AutoCadastroRequest req, HttpServletRequest request) {
        boolean emailExiste = usuarioRepository.existsByEmailIgnoreCase(req.email());
        boolean matriculaExiste = alunoRepository.existsByMatricula(req.matricula());

        if (emailExiste || matriculaExiste) {
            // Não vazar existência de conta (RNF25): registra tentativa e sai com sucesso neutro.
            auditoria.registrar("CADASTRO_DUPLICADO", "usuario", null, null, request,
                    "auto-cadastro com e-mail ou matrícula já existentes");
            return;
        }

        Aluno aluno = new Aluno();
        aluno.setNome(req.nome());
        aluno.setEmail(req.email());
        aluno.setSenhaHash(passwordEncoder.encode(req.senha()));
        aluno.setMatricula(req.matricula());
        aluno.setPeriodo(req.periodo());
        aluno.setTurma(req.turma());
        aluno.setTelefone(req.telefone());
        aluno.setStatus(StatusUsuario.PENDENTE);
        aluno.setPerfil(perfilRepository.findByNome("ALUNO").orElseThrow());

        alunoRepository.save(aluno);
        auditoria.registrar("CRIAR", "usuario", aluno.getId(), null, request,
                "auto-cadastro de aluno — aguardando aprovação");
    }
}
