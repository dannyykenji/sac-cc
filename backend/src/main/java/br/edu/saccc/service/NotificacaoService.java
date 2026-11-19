package br.edu.saccc.service;

import br.edu.saccc.model.Notificacao;
import br.edu.saccc.model.TipoNotificacao;
import br.edu.saccc.repository.NotificacaoRepository;
import org.springframework.stereotype.Service;

/**
 * Notificações do canal SISTEMA (RF56/60) — usadas pelos fluxos de
 * agendamento/cancelamento/atendimento. SMTP continua pendência #T5.
 */
@Service
public class NotificacaoService {

    private final NotificacaoRepository notificacoes;

    public NotificacaoService(NotificacaoRepository notificacoes) {
        this.notificacoes = notificacoes;
    }

    public void notificar(Long destinatarioId, Long agendamentoId, TipoNotificacao tipo,
                          String assunto, String mensagem) {
        Notificacao n = new Notificacao();
        n.setDestinatarioId(destinatarioId);
        n.setAgendamentoId(agendamentoId);
        n.setTipo(tipo);
        n.setAssunto(assunto);
        n.setMensagem(mensagem);
        notificacoes.save(n);  // canal SISTEMA (RF56/60); SMTP é #T5
    }
}
