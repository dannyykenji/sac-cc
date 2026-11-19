package br.edu.saccc.service;

import br.edu.saccc.dto.UsuarioAdminRequest;
import br.edu.saccc.model.Administrador;
import br.edu.saccc.model.Aluno;
import br.edu.saccc.model.Coordenador;
import br.edu.saccc.model.PerfilAcesso;
import br.edu.saccc.model.StatusUsuario;
import br.edu.saccc.model.Usuario;
import br.edu.saccc.repository.AlunoRepository;
import br.edu.saccc.repository.PerfilAcessoRepository;
import br.edu.saccc.repository.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestão de usuários e cadastros pendentes (UC11, ADR-006).
 * Regras: sem exclusão física (RNF60); último ADMIN ativo não pode ser desativado;
 * duplicidade de e-mail/matrícula rejeitada (UC11).
 */
@Service
@PreAuthorize("hasAuthority('GERENCIAR_USUARIOS')")
public class UsuarioAdminService {

    private final UsuarioRepository usuarioRepository;
    private final AlunoRepository alunoRepository;
    private final PerfilAcessoRepository perfilRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaService auditoria;

    public UsuarioAdminService(UsuarioRepository usuarioRepository,
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

    @Transactional(readOnly = true)
    public Page<Aluno> cadastrosPendentes(Pageable pageable) {
        return alunoRepository.findByStatus(StatusUsuario.PENDENTE, pageable);
    }

    @Transactional
    public void aprovar(Long id, Long adminId, HttpServletRequest request) {
        Aluno aluno = alunoPendente(id);
        aluno.setStatus(StatusUsuario.ATIVO);
        auditoria.registrar("APROVAR", "usuario", id, adminId, request, "cadastro de aluno aprovado");
    }

    @Transactional
    public void rejeitar(Long id, String motivo, Long adminId, HttpServletRequest request) {
        Aluno aluno = alunoPendente(id);
        aluno.setStatus(StatusUsuario.INATIVO); // registro preservado (RNF60)
        auditoria.registrar("REJEITAR", "usuario", id, adminId, request, "motivo: " + motivo);
    }

    @Transactional
    public void desativar(Long id, Long adminId, HttpServletRequest request) {
        Usuario u = buscar(id);
        if (u.getPerfil().getNome().equals("ADMIN")
                && usuarioRepository.countByPerfilNomeAndStatus("ADMIN", StatusUsuario.ATIVO) <= 1) {
            throw new IllegalStateException("Não é possível desativar o último administrador ativo.");
        }
        u.setStatus(StatusUsuario.INATIVO);
        auditoria.registrar("DESATIVAR", "usuario", id, adminId, request, null);
    }

    @Transactional
    public void ativar(Long id, Long adminId, HttpServletRequest request) {
        Usuario u = buscar(id);
        u.setStatus(StatusUsuario.ATIVO);
        auditoria.registrar("ATIVAR", "usuario", id, adminId, request, null);
    }

    @Transactional
    public Usuario criar(UsuarioAdminRequest req, Long adminId, HttpServletRequest request) {
        if (usuarioRepository.existsByEmailIgnoreCase(req.email())) {
            throw new IllegalArgumentException("E-mail já cadastrado.");
        }
        PerfilAcesso perfil = perfilRepository.findByNome(req.perfil()).orElseThrow();
        Usuario u = switch (req.perfil()) {
            case "ALUNO" -> novoAluno(req);
            case "COORDENADOR" -> novoCoordenador(req);
            case "ADMIN" -> novoAdministrador(req);
            default -> throw new IllegalArgumentException("Perfil inválido.");
        };
        u.setNome(req.nome());
        u.setEmail(req.email());
        u.setSenhaHash(passwordEncoder.encode(req.senha()));
        u.setPerfil(perfil);
        u.setStatus(StatusUsuario.ATIVO);
        Usuario salvo = usuarioRepository.save(u);
        auditoria.registrar("CRIAR", "usuario", salvo.getId(), adminId, request, "criado via administração");
        return salvo;
    }

    @Transactional
    public Usuario editar(Long id, UsuarioAdminRequest req, Long adminId, HttpServletRequest request) {
        Usuario u = buscar(id);
        usuarioRepository.findByEmailIgnoreCase(req.email())
                .filter(outro -> !outro.getId().equals(id))
                .ifPresent(outro -> { throw new IllegalArgumentException("E-mail já cadastrado."); });
        u.setNome(req.nome());
        u.setEmail(req.email());
        if (req.senha() != null && !req.senha().isBlank()) {
            u.setSenhaHash(passwordEncoder.encode(req.senha()));
        }
        auditoria.registrar("ALTERAR", "usuario", id, adminId, request, null);
        return u;
    }

    @Transactional(readOnly = true)
    public Page<Usuario> listar(String perfil, StatusUsuario status, String busca, Pageable pageable) {
        String termo = busca == null || busca.isBlank() ? null : busca.trim().toLowerCase();
        org.springframework.data.jpa.domain.Specification<Usuario> spec =
                org.springframework.data.jpa.domain.Specification.where(null);
        if (perfil != null && !perfil.isBlank()) {
            String p = perfil;
            spec = spec.and((r, q, cb) -> cb.equal(r.get("perfil").get("nome"), p));
        }
        if (status != null) {
            spec = spec.and((r, q, cb) -> cb.equal(r.get("status"), status));
        }
        if (termo != null) {
            spec = spec.and((r, q, cb) -> cb.or(
                    cb.like(cb.lower(r.get("nome")), "%" + termo + "%"),
                    cb.like(cb.lower(r.get("email")), "%" + termo + "%")));
        }
        return usuarioRepository.findAll(spec, pageable);
    }

    @Transactional(readOnly = true)
    public Usuario buscar(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado."));
    }

    private Aluno alunoPendente(Long id) {
        Aluno aluno = alunoRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Cadastro pendente não encontrado."));
        if (aluno.getStatus() != StatusUsuario.PENDENTE) {
            throw new IllegalStateException("Cadastro já foi processado.");
        }
        return aluno;
    }

    private Aluno novoAluno(UsuarioAdminRequest req) {
        String matricula = req.extras() != null ? req.extras().get("matricula") : null;
        if (matricula == null || matricula.isBlank()) {
            throw new IllegalArgumentException("Matrícula é obrigatória para alunos.");
        }
        if (alunoRepository.existsByMatricula(matricula)) {
            throw new IllegalArgumentException("Matrícula já cadastrada.");
        }
        Aluno a = new Aluno();
        a.setMatricula(matricula);
        if (req.extras() != null) {
            if (req.extras().get("turma") != null) a.setTurma(req.extras().get("turma"));
            if (req.extras().get("curso") != null) a.setCurso(req.extras().get("curso"));
            String periodo = req.extras().get("periodo");
            if (periodo != null) a.setPeriodo(Short.valueOf(periodo));
        }
        return a;
    }

    private Coordenador novoCoordenador(UsuarioAdminRequest req) {
        Coordenador c = new Coordenador();
        if (req.extras() != null) {
            c.setRegistroFuncional(req.extras().get("registroFuncional"));
            c.setCargo(req.extras().get("cargo"));
            c.setAreaResponsabilidade(req.extras().get("areaResponsabilidade"));
        }
        return c;
    }

    private Administrador novoAdministrador(UsuarioAdminRequest req) {
        return new Administrador();
    }
}
