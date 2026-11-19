package br.edu.saccc.service;

import br.edu.saccc.model.Auditoria;
import br.edu.saccc.repository.AuditoriaRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Registro de trilhas de auditoria (RF61–RF64, RNF26). */
@Service
public class AuditoriaService {

    private final AuditoriaRepository repository;

    public AuditoriaService(AuditoriaRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public void registrar(String operacao, String entidade, Long entidadeId,
                          Long usuarioId, HttpServletRequest request, String detalhes) {
        Auditoria a = new Auditoria();
        a.setOperacao(operacao);
        a.setEntidade(entidade);
        a.setIdentificadorEntidade(entidadeId);
        a.setUsuarioId(usuarioId);
        a.setEnderecoIp(ipDe(request));
        a.setDetalhes(detalhes);
        repository.save(a);
    }

    private String ipDe(HttpServletRequest request) {
        if (request == null) return null;
        String fwd = request.getHeader("X-Forwarded-For");
        return fwd != null ? fwd.split(",")[0].trim() : request.getRemoteAddr();
    }
}
