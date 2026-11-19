package br.edu.saccc.controller;

import br.edu.saccc.dto.AgendarRequest;
import br.edu.saccc.dto.AgendamentoResponse;
import br.edu.saccc.model.Usuario;
import br.edu.saccc.repository.UsuarioRepository;
import br.edu.saccc.service.AgendamentoService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.Map;

/** UC03–UC05: agendar, consultar meus agendamentos, cancelar. */
@RestController
@RequestMapping("/api/agendamentos")
public class AgendamentoController {

    private final AgendamentoService service;
    private final UsuarioRepository usuarios;

    public AgendamentoController(AgendamentoService service, UsuarioRepository usuarios) {
        this.service = service;
        this.usuarios = usuarios;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('AGENDAR_ATENDIMENTO')")
    public ResponseEntity<AgendamentoResponse> agendar(@Valid @RequestBody AgendarRequest req,
                                                       HttpServletRequest request, Principal principal) {
        Usuario aluno = usuarioPor(principal);
        AgendamentoResponse resp = AgendamentoResponse.de(
                service.agendar(aluno.getId(), req.disponibilidadeId(), req.modalidade(),
                        req.categoriaId(), req.assunto(), req.descricao(), request, aluno.getId()));
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @GetMapping("/proximos")
    @PreAuthorize("hasAuthority('AGENDAR_ATENDIMENTO')")
    public List<AgendamentoResponse> proximos(Principal principal) {
        return service.proximosDoAluno(usuarioPor(principal).getId())
                .stream().map(AgendamentoResponse::de).toList();
    }

    @GetMapping("/historico")
    @PreAuthorize("hasAuthority('AGENDAR_ATENDIMENTO')")
    public List<AgendamentoResponse> historico(Principal principal) {
        return service.historicoDoAluno(usuarioPor(principal).getId())
                .stream().map(AgendamentoResponse::de).toList();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('AGENDAR_ATENDIMENTO')")
    public ResponseEntity<?> detalhe(@PathVariable Long id, Principal principal) {
        Usuario aluno = usuarioPor(principal);
        if (!service.pertenceAoAluno(id, aluno.getId())) {
            // RNF18: aluno acessa apenas o próprio agendamento; 404 neutro (não vazar existência)
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(AgendamentoResponse.de(service.buscar(id)));
    }

    @PostMapping("/{id}/cancelar")
    @PreAuthorize("hasAuthority('CANCELAR_AGENDAMENTO') or hasAuthority('GERENCIAR_DISPONIBILIDADE')")
    public ResponseEntity<?> cancelar(@PathVariable Long id,
                                      @RequestBody(required = false) Map<String, String> body,
                                      HttpServletRequest request, Authentication authentication, Principal principal) {
        boolean pelaCoordenacao = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("GERENCIAR_DISPONIBILIDADE"))
                && !service.pertenceAoAluno(id, usuarioPor(principal).getId());
        String motivo = body != null ? body.get("motivo") : null;
        AgendamentoResponse resp = AgendamentoResponse.de(
                service.cancelar(id, usuarioPor(principal).getId(), pelaCoordenacao, motivo, request));
        return ResponseEntity.ok(resp);
    }

    private Usuario usuarioPor(Principal principal) {
        return usuarios.findByEmailIgnoreCase(principal.getName()).orElseThrow();
    }
}
