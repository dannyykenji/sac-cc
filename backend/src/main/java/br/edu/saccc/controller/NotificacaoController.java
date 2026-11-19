package br.edu.saccc.controller;

import br.edu.saccc.model.Notificacao;
import br.edu.saccc.model.Usuario;
import br.edu.saccc.repository.NotificacaoRepository;
import br.edu.saccc.repository.UsuarioRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

/** RF56/RF60 — usuário vê as PRÓPRIAS notificações (canal SISTEMA; SMTP é #T5). */
@RestController
@RequestMapping("/api/notificacoes")
@PreAuthorize("isAuthenticated()")
public class NotificacaoController {

    private final NotificacaoRepository notificacoes;
    private final UsuarioRepository usuarios;

    public NotificacaoController(NotificacaoRepository notificacoes, UsuarioRepository usuarios) {
        this.notificacoes = notificacoes;
        this.usuarios = usuarios;
    }

    public record NotificacaoItem(Long id, String tipo, String assunto, String mensagem,
                                  String statusEnvio, String dataCriacao) {
        public static NotificacaoItem de(Notificacao n) {
            return new NotificacaoItem(n.getId(), n.getTipo().name(), n.getAssunto(), n.getMensagem(),
                    n.getStatusEnvio(),
                    n.getDataCriacao() == null ? null : n.getDataCriacao().toString());
        }
    }

    @GetMapping
    public List<NotificacaoItem> minhas(Principal principal) {
        Usuario u = usuarios.findByEmailIgnoreCase(principal.getName()).orElseThrow();
        return notificacoes.findByDestinatarioIdOrderByDataCriacaoDesc(u.getId())
                .stream().map(NotificacaoItem::de).toList();
    }
}
