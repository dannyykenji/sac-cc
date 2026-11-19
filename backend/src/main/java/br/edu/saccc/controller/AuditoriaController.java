package br.edu.saccc.controller;

import br.edu.saccc.model.Auditoria;
import br.edu.saccc.model.Usuario;
import br.edu.saccc.repository.AuditoriaRepository;
import br.edu.saccc.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

/** RF64/RNF26 — consulta da trilha de auditoria (ADMIN). Sem dados sensíveis nas respostas. */
@RestController
@RequestMapping("/api/admin/auditoria")
@PreAuthorize("hasAuthority('GERENCIAR_USUARIOS')")
public class AuditoriaController {

    private final AuditoriaRepository auditoria;
    private final UsuarioRepository usuarios;

    public AuditoriaController(AuditoriaRepository auditoria, UsuarioRepository usuarios) {
        this.auditoria = auditoria;
        this.usuarios = usuarios;
    }

    public record AuditoriaItem(Long id, Long usuarioId, String operacao, String entidade,
                                Long identificadorEntidade, String dataHora, String enderecoIp, String detalhes) {
        public static AuditoriaItem de(Auditoria a) {
            return new AuditoriaItem(a.getId(), a.getUsuarioId(), a.getOperacao(), a.getEntidade(),
                    a.getIdentificadorEntidade(),
                    a.getDataHora() == null ? null : a.getDataHora().toString(),
                    a.getEnderecoIp(), a.getDetalhes());
        }
    }

    @GetMapping
    public Page<AuditoriaItem> listar(@RequestParam(required = false) String entidade,
                                      @RequestParam(required = false) Long usuarioId,
                                      @PageableDefault(size = 20) Pageable pageable) {
        Page<Auditoria> pagina;
        if (entidade != null && usuarioId != null) {
            pagina = auditoria.findByEntidadeOrUsuarioIdOrderByDataHoraDesc(entidade, usuarioId, pageable);
        } else if (entidade != null) {
            pagina = auditoria.findByEntidadeOrderByDataHoraDesc(entidade, pageable);
        } else {
            pagina = auditoria.findAllByOrderByDataHoraDesc(pageable);
        }
        return pagina.map(AuditoriaItem::de);
    }

    private Long idDe(Principal principal) {
        return usuarios.findByEmailIgnoreCase(principal.getName()).map(Usuario::getId).orElse(null);
    }
}
