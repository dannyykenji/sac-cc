package br.edu.saccc.service;

import br.edu.saccc.model.Agendamento;
import br.edu.saccc.model.MensagemChat;
import br.edu.saccc.model.StatusAgendamento;
import br.edu.saccc.repository.AgendamentoRepository;
import br.edu.saccc.repository.MensagemChatRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Chat do atendimento online (ADR-008): sala = agendamento, janela parametrizada,
 * mensagens append-only. Estado da sala é DERIVADO (nunca persistido).
 */
@Service
public class ChatService {

    // fuso configurável (RNF62): ZoneSaoPaulo.ZONE lê SACCC_TIMEZONE

    /** Estados derivados da sala (ADR-008). */
    public enum EstadoSala { ABERTA, FECHADA, ENCERRADA }

    private final AgendamentoRepository agendamentos;
    private final MensagemChatRepository mensagens;
    private final ParametroService parametros;

    public ChatService(AgendamentoRepository agendamentos,
                       MensagemChatRepository mensagens,
                       ParametroService parametros) {
        this.agendamentos = agendamentos;
        this.mensagens = mensagens;
        this.parametros = parametros;
    }

    /** Regras de acesso (RNF17/18): aluno só na própria sala; coordenação em qualquer uma. */
    @Transactional(readOnly = true)
    public void validarAcesso(Long agendamentoId, Long usuarioId, boolean ehCoordenacao) {
        Agendamento ag = agendamentos.findById(agendamentoId)
                .orElseThrow(() -> new EntityNotFoundException("Sala não encontrada."));
        if (!ehCoordenacao && !ag.getAlunoId().equals(usuarioId)) {
            // 404 neutro: não vazar existência de sala alheia
            throw new EntityNotFoundException("Sala não encontrada.");
        }
    }

    @Transactional(readOnly = true)
    public EstadoSala estadoDaSala(Long agendamentoId) {
        Agendamento ag = agendamentos.findById(agendamentoId)
                .orElseThrow(() -> new EntityNotFoundException("Sala não encontrada."));
        // atendimento registrado → encerrada (somente leitura)
        if (ag.getStatus() != StatusAgendamento.SOLICITADO && ag.getStatus() != StatusAgendamento.CONFIRMADO) {
            return EstadoSala.ENCERRADA;
        }
        // modo teste (deploy de avaliação): sala ABERTA desde a criação, sem janela;
        // em uso real CHAT_MODO_TESTE='false' restaura a janela parametrizada (ADR-008)
        if (modoTeste()) {
            return EstadoSala.ABERTA;
        }
        LocalDateTime agora = LocalDateTime.now(ZoneSaoPaulo.ZONE);
        LocalDateTime inicio = LocalDateTime.of(ag.getData(), ag.getHoraInicio());
        LocalDateTime fim = LocalDateTime.of(ag.getData(), ag.getHoraFim());
        long abreAntes = parametros.longo("CHAT_ABRE_MIN_ANTES", 10);
        long fechaDepois = parametros.longo("CHAT_FECHA_MIN_DEPOIS", 15);
        if (agora.isBefore(inicio.minusMinutes(abreAntes))) {
            return EstadoSala.FECHADA;   // ainda não abriu
        }
        if (agora.isAfter(fim.plusMinutes(fechaDepois))) {
            return EstadoSala.FECHADA;   // janela expirou sem registro do atendimento
        }
        return EstadoSala.ABERTA;
    }

    /** Deploy de avaliação: sala aberta desde a criação (ver estadoDaSala). */
    public boolean modoTeste() {
        return parametros.booleano("CHAT_MODO_TESTE", false);
    }

    /** Envio (REST → persiste → difunde via WS no controller). Append-only. */
    @Transactional
    public MensagemChat enviar(Long agendamentoId, Long autorId, boolean ehCoordenacao, String conteudo) {
        if (conteudo == null || conteudo.isBlank()) {
            throw new br.edu.saccc.exception.ValidacaoException("Mensagem vazia.");
        }
        String texto = conteudo.strip();
        if (texto.length() > 2000) {
            throw new br.edu.saccc.exception.ValidacaoException("Mensagem excede 2000 caracteres.");
        }
        validarAcesso(agendamentoId, autorId, ehCoordenacao);
        EstadoSala estado = estadoDaSala(agendamentoId);
        if (estado != EstadoSala.ABERTA) {
            throw new IllegalStateException(switch (estado) {
                case ENCERRADA -> "Atendimento já registrado: a sala está encerrada (somente leitura).";
                default -> "A sala ainda não está aberta. Ela abre alguns minutos antes do horário marcado.";
            });
        }
        MensagemChat m = new MensagemChat();
        m.setAgendamentoId(agendamentoId);
        m.setAutorId(autorId);
        m.setConteudo(texto);
        return mensagens.save(m);
    }

    /** Histórico: leitura permitida a qualquer momento pelas partes (registro do atendimento). */
    @Transactional(readOnly = true)
    public List<MensagemChat> historico(Long agendamentoId, Long usuarioId, boolean ehCoordenacao) {
        validarAcesso(agendamentoId, usuarioId, ehCoordenacao);
        return mensagens.findByAgendamentoIdOrderByDataEnvioAsc(agendamentoId);
    }
}
