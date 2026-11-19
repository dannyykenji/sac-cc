package br.edu.saccc.controller;

import br.edu.saccc.dto.UsuarioAdminRequest;
import br.edu.saccc.dto.UsuarioAdminResponse;
import br.edu.saccc.model.StatusUsuario;
import br.edu.saccc.model.Usuario;
import br.edu.saccc.service.UsuarioAdminService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** UC11 + ADR-006: gestão de usuários e fila de aprovação (ADMIN). */
@RestController
@RequestMapping("/api")
@PreAuthorize("hasAuthority('GERENCIAR_USUARIOS')")
public class UsuarioAdminController {

    private final UsuarioAdminService service;
    private final br.edu.saccc.repository.UsuarioRepository usuarioRepository;

    public UsuarioAdminController(UsuarioAdminService service,
                                  br.edu.saccc.repository.UsuarioRepository usuarioRepository) {
        this.service = service;
        this.usuarioRepository = usuarioRepository;
    }

    // ----- fila de aprovação (ADR-006) -----

    @GetMapping("/admin/cadastros-pendentes")
    public Page<UsuarioAdminResponse> pendentes(@PageableDefault(size = 20) Pageable pageable) {
        return service.cadastrosPendentes(pageable).map(UsuarioAdminResponse::de);
    }

    @PostMapping("/admin/cadastros-pendentes/{id}/aprovar")
    public ResponseEntity<Void> aprovar(@PathVariable Long id, HttpServletRequest request,
                                        java.security.Principal principal) {
        service.aprovar(id, usuarioId(principal), request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/admin/cadastros-pendentes/{id}/rejeitar")
    public ResponseEntity<?> rejeitar(@PathVariable Long id,
                                         @RequestBody Map<String, String> body,
                                         HttpServletRequest request,
                                         java.security.Principal principal) {
        String motivo = body.get("motivo");
        if (motivo == null || motivo.isBlank()) {
            org.springframework.http.ProblemDetail pd = org.springframework.http.ProblemDetail
                    .forStatusAndDetail(HttpStatus.BAD_REQUEST, "O motivo da rejeição é obrigatório.");
            pd.setTitle("Dados inválidos");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(pd);
        }
        service.rejeitar(id, motivo, usuarioId(principal), request);
        return ResponseEntity.noContent().build();
    }

    // ----- gestão de usuários (UC11) -----

    @GetMapping("/usuarios")
    public Page<UsuarioAdminResponse> listar(@RequestParam(required = false) String perfil,
                                             @RequestParam(required = false) StatusUsuario status,
                                             @RequestParam(required = false) String busca,
                                             @PageableDefault(size = 20) Pageable pageable) {
        return service.listar(perfil, status, busca, pageable).map(UsuarioAdminResponse::de);
    }

    @GetMapping("/usuarios/{id}")
    public UsuarioAdminResponse detalhe(@PathVariable Long id) {
        return UsuarioAdminResponse.de(service.buscar(id));
    }

    @PostMapping("/usuarios")
    public ResponseEntity<UsuarioAdminResponse> criar(@Valid @RequestBody UsuarioAdminRequest req,
                                                      HttpServletRequest request,
                                                      java.security.Principal principal) {
        Usuario u = service.criar(req, usuarioId(principal), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(UsuarioAdminResponse.de(u));
    }

    @PutMapping("/usuarios/{id}")
    public UsuarioAdminResponse editar(@PathVariable Long id,
                                       @Valid @RequestBody UsuarioAdminRequest req,
                                       HttpServletRequest request,
                                       java.security.Principal principal) {
        return UsuarioAdminResponse.de(service.editar(id, req, usuarioId(principal), request));
    }

    @PostMapping("/usuarios/{id}/desativar")
    public ResponseEntity<?> desativar(@PathVariable Long id, HttpServletRequest request,
                                       java.security.Principal principal) {
        try {
            service.desativar(id, usuarioId(principal), request);
            return ResponseEntity.noContent().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("title", "Operação não permitida", "detail", e.getMessage()));
        }
    }

    @PostMapping("/usuarios/{id}/ativar")
    public ResponseEntity<Void> ativar(@PathVariable Long id, HttpServletRequest request,
                                       java.security.Principal principal) {
        service.ativar(id, usuarioId(principal), request);
        return ResponseEntity.noContent().build();
    }

    private Long usuarioId(java.security.Principal principal) {
        return usuarioRepository.findByEmailIgnoreCase(principal.getName())
                .map(Usuario::getId).orElse(null);
    }
}
