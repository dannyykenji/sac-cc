package br.edu.saccc.service;

import br.edu.saccc.model.Agendamento;
import br.edu.saccc.model.StatusAgendamento;
import br.edu.saccc.repository.AgendamentoRepository;
import br.edu.saccc.repository.DisponibilidadeRepository;
import br.edu.saccc.repository.NotificacaoRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * #N9 — lembrete de chat iminente (canal SISTEMA, RF56/60): quando um
 * atendimento ONLINE está a N minutos do início (parametro
 * LEMBRETE_CHAT_MIN_ANTES, default 15), cria notificação no sino para
 * o ALUNO e para a COORDENAÇÃO dona do slot — uma vez só (idempotente
 * por janela: percorre apenas a faixa [agora, agora+15min]).
 */
@Service
public class LembreteChatService {

    private final AgendamentoRepository agendamentos;
    private final NotificacaoRepository notificacoes;
    private final DisponibilidadeRepository disponibilidade;
    private final ParametroService parametros;
    private final NotificacaoService notificacaoService;

    public LembreteChatService(AgendamentoRepository agendamentos,
                               NotificacaoRepository notificacoes,
                               DisponibilidadeRepository disponibilidade,
                               ParametroService parametros,
                               NotificacaoService notificacaoService) {
        this.agendamentos = agendamentos;
        this.notificacoes = notificacoes;
        this.disponibilidade = disponibilidade;
        this.parametros = parametros;
        this.notificacaoService = notificacaoService;
    }

    /** A cada minuto: lembretes para atendimentos ONLINE começando em breve. */
    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void avisarChatIminente() {
        long janelaMin = parametros.longo("LEMBRETE_CHAT_MIN_ANTES", 15);
        LocalDateTime agora = LocalDateTime.now(ZoneSaoPaulo.ZONE).truncatedTo(ChronoUnit.MINUTES);
        LocalDateTime limite = agora.plusMinutes(janelaMin);

        // ativos ONLINE cujo início cai na janela (uma única varredura — o job
        // roda a cada minuto, cada minuto só é "varrido" uma vez)
        List<Agendamento> iminentes = agendamentos
                .findByStatusInAndModalidadeAndDataAndHoraInicioBetween(
                        List.of(StatusAgendamento.SOLICITADO, StatusAgendamento.CONFIRMADO),
                        br.edu.saccc.model.ModalidadeAtendimento.ONLINE,
                        agora.toLocalDate(), agora.toLocalTime(), limite.toLocalTime());

        for (Agendamento ag : iminentes) {
            long faltamMin = ChronoUnit.MINUTES.between(agora,
                    LocalDateTime.of(ag.getData(), ag.getHoraInicio()));
            String quando = faltamMin <= 0 ? "agora" : "em " + faltamMin + " min";
            String msg = "Atendimento ONLINE %s (%s). A sala abre 10 minutos antes do horário."
                    .formatted(quando, ag.getProtocolo());

            // aluno
            notificacaoService.notificar(ag.getAlunoId(), ag.getId(),
                    br.edu.saccc.model.TipoNotificacao.LEMBRETE,
                    "Conversa com a coordenação em breve", msg);
            // coordenação dona do slot
            disponibilidade.findById(ag.getDisponibilidadeId()).ifPresent(d ->
                    notificacaoService.notificar(d.getCriadoPorId(), ag.getId(),
                            br.edu.saccc.model.TipoNotificacao.LEMBRETE,
                            "Atendimento online em breve",
                            "Protocolo %s — a conversa começa %s.".formatted(ag.getProtocolo(), quando)));
        }
    }
}
