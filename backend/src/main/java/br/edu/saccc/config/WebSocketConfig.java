package br.edu.saccc.config;

import br.edu.saccc.model.Usuario;
import br.edu.saccc.repository.UsuarioRepository;
import br.edu.saccc.service.ChatService;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

import java.security.Principal;

/**
 * WebSocket+STOMP do chat (ADR-008).
 * O handshake /ws é público (SockJS), mas cada SUBSCRIBE em /topic/agendamento/{id}
 * é autorizado NO SERVIDOR (RNF17/18): aluno somente na própria sala (404 neutro),
 * coordenação (REGISTRAR_ATENDIMENTO) em qualquer sala. O Principal vem da sessão
 * HTTP carregada no handshake (mesmo cookie SESSIONID do Spring Security).
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private static final String PREFIXO_TOPICO = "/topic/agendamento/";

    private final ChatService chatService;
    private final UsuarioRepository usuarios;

    public WebSocketConfig(ChatService chatService, UsuarioRepository usuarios) {
        this.chatService = chatService;
        this.usuarios = usuarios;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor =
                        MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
                if (accessor != null && StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
                    String destino = accessor.getDestination();
                    if (destino != null && destino.startsWith(PREFIXO_TOPICO)) {
                        Long agendamentoId = parseAgendamentoId(destino);
                        Principal user = accessor.getUser();
                        if (!(user instanceof Authentication auth) || !auth.isAuthenticated()) {
                            throw new MessagingException("Sessão não autenticada.");
                        }
                        Usuario u = usuarios.findByEmailIgnoreCase(auth.getName()).orElseThrow();
                        boolean ehCoordenacao = auth.getAuthorities().stream()
                                .anyMatch(a -> a.getAuthority().equals("REGISTRAR_ATENDIMENTO"));
                        // valida no servidor: aluno só na própria sala (exceção → assinatura rejeitada)
                        chatService.validarAcesso(agendamentoId, u.getId(), ehCoordenacao);
                    }
                }
                return message;
            }
        });
    }

    private static Long parseAgendamentoId(String destino) {
        String sufixo = destino.substring(PREFIXO_TOPICO.length());
        if (sufixo.isEmpty() || !sufixo.chars().allMatch(Character::isDigit)) {
            throw new MessagingException("Destino de sala inválido.");
        }
        return Long.valueOf(sufixo);
    }
}
