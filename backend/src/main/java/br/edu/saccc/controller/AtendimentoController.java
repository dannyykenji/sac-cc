package br.edu.saccc.controller;

import br.edu.saccc.dto.RegistrarAtendimentoRequest;
import br.edu.saccc.dto.AtendimentoResponse;
import br.edu.saccc.model.Usuario;
import br.edu.saccc.repository.UsuarioRepository;
import br.edu.saccc.service.AtendimentoService;
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

/** UC08/UC09 — registro de atendimento, evolução de status e histórico (coordenação). */
@RestController
@RequestMapping("/api/atendimentos")
public class AtendimentoController {

    private final AtendimentoService service;
    private final UsuarioRepository usuarios;

    public AtendimentoController(AtendimentoService service, UsuarioRepository usuarios) {
        this.service = service;
        this.usuarios = usuarios;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('REGISTRAR_ATENDIMENTO')")
    public ResponseEntity<AtendimentoResponse> registrar(@Valid @RequestBody RegistrarAtendimentoRequest req,
                                                         HttpServletRequest request, Principal principal) {
        Long responsavel = usuarioId(principal);
        AtendimentoResponse resp = AtendimentoResponse.de(service.registrar(
                req.agendamentoId(), req.realizado(), req.statusResolucao(),
                req.observacoes(), req.solucao(), req.encaminhamento(),
                req.prazoRetorno(), responsavel, request));
        return ResponseEntity.status(HttpStatus.CREATED).body(resp);
    }

    @PostMapping("/{agendamentoId}/status")
    @PreAuthorize("hasAuthority('REGISTRAR_ATENDIMENTO')")
    public AtendimentoResponse atualizarStatus(@PathVariable Long agendamentoId,
                                               @RequestBody AtualizarStatusRequest req,
                                               HttpServletRequest request, Principal principal) {
        java.time.LocalDate prazo = req.prazoRetorno() == null || req.prazoRetorno().isBlank()
                ? null : java.time.LocalDate.parse(req.prazoRetorno());
        return AtendimentoResponse.de(service.atualizarStatus(
                agendamentoId, req.statusResolucao(), req.justificativa(), req.solucao(),
                prazo, usuarioId(principal), request));
    }

    @GetMapping("/{agendamentoId}")
    @PreAuthorize("hasAuthority('REGISTRAR_ATENDIMENTO')")
    public AtendimentoResponse detalhe(@PathVariable Long agendamentoId) {
        return AtendimentoResponse.de(service.buscar(agendamentoId));
    }

    @GetMapping("/{agendamentoId}/historico")
    @PreAuthorize("hasAuthority('REGISTRAR_ATENDIMENTO')")
    public List<AtendimentoResponse.HistoricoItem> historico(@PathVariable Long agendamentoId) {
        return service.historico(agendamentoId).stream()
                .map(AtendimentoResponse.HistoricoItem::de)
                .toList();
    }

    @PostMapping("/{agendamentoId}/confirmar-aluno")
    @PreAuthorize("hasAuthority('AGENDAR_ATENDIMENTO')")
    public AtendimentoResponse confirmarAluno(@PathVariable Long agendamentoId, Principal principal) {
        return AtendimentoResponse.de(service.confirmarComparecimento(agendamentoId, usuarioId(principal)));
    }

    public record AtualizarStatusRequest(String statusResolucao, String justificativa,
                                         String solucao, String prazoRetorno) {}

    private Long usuarioId(Principal principal) {
        return usuarios.findByEmailIgnoreCase(principal.getName())
                .map(Usuario::getId).orElse(null);
    }
}
