package br.edu.saccc.controller;

import br.edu.saccc.model.MensagemChat;
import br.edu.saccc.model.Usuario;
import br.edu.saccc.repository.UsuarioRepository;
import br.edu.saccc.service.ChatService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
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

/**
 * Chat do atendimento (ADR-008): envio via REST (persiste + difunde no tópico STOMP),
 * histórico e estado da sala. Assinatura: /topic/agendamento/{id} (SockJS em /ws).
 */
@RestController
@RequestMapping("/api/agendamentos/{agendamentoId}/chat")
public class ChatController {

    private final ChatService service;
    private final UsuarioRepository usuarios;
    private final SimpMessagingTemplate messaging;

    public ChatController(ChatService service, UsuarioRepository usuarios, SimpMessagingTemplate messaging) {
        this.service = service;
        this.usuarios = usuarios;
        this.messaging = messaging;
    }

    public record EnviarRequest(@Size(min = 1, max = 2000) String conteudo) {}

    public record MensagemResponse(Long id, Long autorId, String autorNome, String perfilAutor,
                                   String conteudo, String dataEnvio) {
        public static MensagemResponse de(MensagemChat m, Usuario autor) {
            return new MensagemResponse(m.getId(), m.getAutorId(),
                    autor != null ? autor.getNome() : "?",
                    autor != null ? autor.getPerfil().getNome() : "?",
                    m.getConteudo(), m.getDataEnvio().toString());
        }
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> enviar(@PathVariable Long agendamentoId,
                                    @Valid @RequestBody EnviarRequest req,
                                    Authentication authentication) {
        Usuario autor = usuarioPor(authentication.getName());
        boolean ehCoordenacao = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("REGISTRAR_ATENDIMENTO"));
        MensagemChat salva = service.enviar(agendamentoId, autor.getId(), ehCoordenacao, req.conteudo());
        MensagemResponse resp = MensagemResponse.de(salva, autor);
        // difusão em tempo real para quem está na sala (ADR-008)
        messaging.convertAndSend("/topic/agendamento/" + agendamentoId, resp);
        return ResponseEntity.status(201).body(resp);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<MensagemResponse> historico(@PathVariable Long agendamentoId, Authentication authentication) {
        Usuario u = usuarioPor(authentication.getName());
        boolean ehCoordenacao = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("REGISTRAR_ATENDIMENTO"));
        return service.historico(agendamentoId, u.getId(), ehCoordenacao).stream()
                .map(m -> MensagemResponse.de(m, usuarioPorId(m.getAutorId())))
                .toList();
    }

    /** Estado atual da sala para a UI (contador até abrir, aviso de encerrada etc.). */
    public record EstadoResponse(String estado, boolean modoTeste) {}

    @GetMapping("/estado")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<EstadoResponse> estado(@PathVariable Long agendamentoId, Authentication authentication) {
        Usuario u = usuarioPor(authentication.getName());
        boolean ehCoordenacao = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("REGISTRAR_ATENDIMENTO"));
        try {
            service.validarAcesso(agendamentoId, u.getId(), ehCoordenacao);
            return ResponseEntity.ok(new EstadoResponse(
                    service.estadoDaSala(agendamentoId).name(), service.modoTeste()));
        } catch (jakarta.persistence.EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }
    }

    private Usuario usuarioPor(String email) {
        return usuarios.findByEmailIgnoreCase(email).orElseThrow();
    }

    private Usuario usuarioPorId(Long id) {
        return usuarios.findById(id).orElse(null);
    }
}
