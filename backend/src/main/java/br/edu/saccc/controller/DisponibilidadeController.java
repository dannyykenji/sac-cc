package br.edu.saccc.controller;

import br.edu.saccc.dto.CriarDisponibilidadeRequest;
import br.edu.saccc.dto.DisponibilidadeResponse;
import br.edu.saccc.model.Disponibilidade;
import br.edu.saccc.model.ModalidadeAtendimento;
import br.edu.saccc.repository.UsuarioRepository;
import br.edu.saccc.service.DisponibilidadeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

/** UC06 — gerenciamento de disponibilidade + consulta (UC02 usa o GET público-aluno). */
@RestController
@RequestMapping("/api/disponibilidades")
public class DisponibilidadeController {

    private final DisponibilidadeService service;
    private final UsuarioRepository usuarios;

    public DisponibilidadeController(DisponibilidadeService service, UsuarioRepository usuarios) {
        this.service = service;
        this.usuarios = usuarios;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('GERENCIAR_DISPONIBILIDADE')")
    public ResponseEntity<List<DisponibilidadeResponse>> criarLote(
            @Valid @RequestBody CriarDisponibilidadeRequest req,
            HttpServletRequest request, Principal principal) {
        Disponibilidade proto = new Disponibilidade();
        proto.setData(req.data());
        proto.setHoraInicio(req.horaInicio());
        proto.setHoraFim(req.horaFim());
        proto.setDuracaoMinutos(req.duracaoMinutos());
        proto.setModalidade(ModalidadeAtendimento.valueOf(req.modalidade()));
        proto.setLocal(req.local());
        proto.setLinkReuniao(req.linkReuniao());
        proto.setObservacao(req.observacao());

        Long criadoPor = usuarios.findByEmailIgnoreCase(principal.getName())
                .map(u -> u.getId()).orElse(null);
        List<Disponibilidade> slots = service.criarLote(proto, criadoPor, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(slots.stream().map(DisponibilidadeResponse::de).toList());
    }

    /** Agenda do aluno: apenas horários DISPONIVEL e futuros (RF15, UC02). */
    @GetMapping("/publicas")
    @PreAuthorize("hasAnyRole('ALUNO','COORDENADOR','ADMIN')")
    public List<DisponibilidadeResponse> paraAluno(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
        return service.listarParaAluno(de, ate).stream().map(DisponibilidadeResponse::de).toList();
    }

    /** Agenda administrativa (RF14): todos os status. */
    @GetMapping
    @PreAuthorize("hasAuthority('GERENCIAR_DISPONIBILIDADE')")
    public List<DisponibilidadeResponse> admin(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate) {
        return service.listarAdmin(de, ate).stream().map(DisponibilidadeResponse::de).toList();
    }

    @PostMapping("/{id}/bloquear")
    @PreAuthorize("hasAuthority('GERENCIAR_DISPONIBILIDADE')")
    public ResponseEntity<Void> bloquear(@PathVariable Long id, HttpServletRequest request, Principal principal) {
        service.bloquear(id, usuarioId(principal), request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/desbloquear")
    @PreAuthorize("hasAuthority('GERENCIAR_DISPONIBILIDADE')")
    public ResponseEntity<Void> desbloquear(@PathVariable Long id, HttpServletRequest request, Principal principal) {
        service.desbloquear(id, usuarioId(principal), request);
        return ResponseEntity.noContent().build();
    }

    private Long usuarioId(Principal principal) {
        return usuarios.findByEmailIgnoreCase(principal.getName())
                .map(u -> u.getId()).orElse(null);
    }
}
