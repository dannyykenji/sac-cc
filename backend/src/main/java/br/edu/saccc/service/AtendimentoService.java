package br.edu.saccc.service;

import br.edu.saccc.exception.ValidacaoException;
import br.edu.saccc.model.Agendamento;
import br.edu.saccc.model.Atendimento;
import br.edu.saccc.model.HistoricoAtendimento;
import br.edu.saccc.model.Notificacao;
import br.edu.saccc.model.StatusAgendamento;
import br.edu.saccc.model.StatusResolucao;
import br.edu.saccc.model.TipoNotificacao;
import br.edu.saccc.repository.AgendamentoRepository;
import br.edu.saccc.repository.AtendimentoRepository;
import br.edu.saccc.repository.HistoricoAtendimentoRepository;
import br.edu.saccc.repository.NotificacaoRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Registro e evolução do atendimento (UC08/UC09, RF31–RF43, RN06–RN14).
 * Campos obrigatórios por status (RN07–09), transições validadas (UC09),
 * histórico APPEND-ONLY (RN13) e notificações registradas (RF56/60).
 */
@Service
public class AtendimentoService {

    // fuso configurável (RNF62): ZoneSaoPaulo.ZONE lê SACCC_TIMEZONE

    private final AtendimentoRepository atendimentos;
    private final AgendamentoRepository agendamentos;
    private final HistoricoAtendimentoRepository historico;
    private final NotificacaoRepository notificacoes;
    private final NotificacaoService servicoNotificacoes;
    private final AuditoriaService auditoria;

    public AtendimentoService(AtendimentoRepository atendimentos,
                              AgendamentoRepository agendamentos,
                              HistoricoAtendimentoRepository historico,
                              NotificacaoRepository notificacoes,
                              AuditoriaService auditoria,
                              NotificacaoService servicoNotificacoes) {
        this.atendimentos = atendimentos;
        this.agendamentos = agendamentos;
        this.historico = historico;
        this.notificacoes = notificacoes;
        this.auditoria = auditoria;
        this.servicoNotificacoes = servicoNotificacoes;
    }

    /** UC08 — despacho: realizado → fluxo normal; ausência → categoria separada (RN12). */
    @Transactional
    @PreAuthorize("hasAuthority('REGISTRAR_ATENDIMENTO')")
    public Atendimento registrar(Long agendamentoId, boolean realizado, String statusResolucaoStr,
                                 String observacoes, String solucao, String encaminhamento,
                                 LocalDate prazoRetorno, Long responsavelId, HttpServletRequest request) {
        if (!realizado) {
            if (statusResolucaoStr != null && !statusResolucaoStr.isBlank()) {
                throw new ValidacaoException("Ausência não recebe status de resolução (RN12).");
            }
            return registrarAusencia(agendamentoId, observacoes, responsavelId, request);
        }
        Agendamento ag = agendamentos.findById(agendamentoId)
                .orElseThrow(() -> new EntityNotFoundException("Agendamento não encontrado."));
        if (ag.getStatus() != StatusAgendamento.SOLICITADO && ag.getStatus() != StatusAgendamento.CONFIRMADO) {
            throw new IllegalStateException("Agendamento não está ativo (já " + ag.getStatus() + ").");
        }
        if (atendimentos.existsById(agendamentoId)) {
            throw new IllegalStateException("Atendimento já registrado para este agendamento.");
        }
        return registrarRealizado(ag, realizado, statusResolucaoStr, observacoes, solucao,
                encaminhamento, prazoRetorno, responsavelId, request);
    }

    /** UC08 fluxo normal: atendimento ocorrido. */
    @Transactional
    @PreAuthorize("hasAuthority('REGISTRAR_ATENDIMENTO')")
    public Atendimento registrarRealizado(Agendamento ag, boolean realizado, String statusResolucaoStr,
                                          String observacoes, String solucao, String encaminhamento,
                                          LocalDate prazoRetorno, Long responsavelId, HttpServletRequest request) {
        StatusResolucao status = parseStatus(statusResolucaoStr);
        validarCamposPorStatus(status, solucao, observacoes, prazoRetorno);

        Atendimento at = new Atendimento();
        at.setAgendamentoId(ag.getId());
        at.setResponsavelId(responsavelId);
        at.setRealizado(true);
        at.setStatusResolucao(status);
        at.setObservacoes(observacoes);
        at.setSolucaoRegistrada(solucao);
        at.setEncaminhamento(encaminhamento);
        at.setPrazoRetorno(prazoRetorno);
        if (status != StatusResolucao.EM_TRATATIVA) {
            at.setDataEncerramento(LocalDateTime.now(ZoneSaoPaulo.ZONE));
        }
        atendimentos.save(at);

        // agendamento evolui (UC08): REALIZADO
        ag.setStatus(StatusAgendamento.REALIZADO);

        registrarHistorico(at.getAgendamentoId(), null, status.name(),
                "Atendimento registrado: " + status.name(), responsavelId);
        servicoNotificacoes.notificar(ag.getAlunoId(), ag.getId(), TipoNotificacao.STATUS,
                "Atendimento registrado",
                "Seu atendimento de " + ag.getData() + " foi registrado pela coordenação.");
        auditoria.registrar("REGISTRAR", "atendimento", at.getAgendamentoId(), responsavelId, request,
                "status=" + status);
        return at;
    }

    /** UC08 alternativo — ausência (RN12): categoria separada, sem resolução. */
    @Transactional
    @PreAuthorize("hasAuthority('REGISTRAR_ATENDIMENTO')")
    public Atendimento registrarAusencia(Long agendamentoId, String observacoes,
                                         Long responsavelId, HttpServletRequest request) {
        Agendamento ag = agendamentos.findById(agendamentoId)
                .orElseThrow(() -> new EntityNotFoundException("Agendamento não encontrado."));
        if (ag.getStatus() != StatusAgendamento.SOLICITADO && ag.getStatus() != StatusAgendamento.CONFIRMADO) {
            throw new IllegalStateException("Agendamento não está ativo.");
        }
        if (atendimentos.existsById(agendamentoId)) {
            throw new IllegalStateException("Atendimento já registrado para este agendamento.");
        }

        Atendimento at = new Atendimento();
        at.setAgendamentoId(agendamentoId);
        at.setResponsavelId(responsavelId);
        at.setRealizado(false);
        // RN12: ausência não participa dos três status — usa AUSENCIA (domínio estendido interno)
        at.setStatusResolucao(StatusResolucao.AUSENCIA);
        at.setObservacoes(observacoes);
        atendimentos.save(at);

        ag.setStatus(StatusAgendamento.NAO_COMPARECEU);

        registrarHistorico(agendamentoId, null, "AUSENCIA",
                "Ausência registrada (aluno não compareceu)", responsavelId);
        servicoNotificacoes.notificar(ag.getAlunoId(), ag.getId(), TipoNotificacao.STATUS,
                "Ausência registrada",
                "Foi registrada ausência no seu atendimento de " + ag.getData() + ".");
        auditoria.registrar("REGISTRAR", "atendimento", agendamentoId, responsavelId, request, "ausência");
        return at;
    }

    /** UC09 — transição de status com validação de campos e histórico (RN07–09, RN13, RN14). */
    @Transactional
    @PreAuthorize("hasAuthority('REGISTRAR_ATENDIMENTO')")
    public Atendimento atualizarStatus(Long agendamentoId, String novoStatusStr,
                                       String justificativa, String solucao,
                                       LocalDate prazoRetorno, Long responsavelId, HttpServletRequest request) {
        Atendimento at = buscar(agendamentoId);
        StatusResolucao novo = parseStatus(novoStatusStr);
        StatusResolucao anterior = at.getStatusResolucao();

        // UC09: encerrado só muda com justificativa (RN14: exceção auditada)
        if (at.getDataEncerramento() != null && justificativa == null) {
            throw new ValidacaoException(
                    "Atendimento encerrado: informe justificativa para alterar o status (RN14).");
        }
        validarCamposPorStatus(novo, solucao != null ? solucao : at.getSolucaoRegistrada(),
                justificativa != null ? justificativa : at.getObservacoes(),
                prazoRetorno != null ? prazoRetorno : at.getPrazoRetorno());

        at.setStatusResolucao(novo);
        if (novo == StatusResolucao.EM_TRATATIVA) {
            at.setDataEncerramento(null);
            if (prazoRetorno != null) at.setPrazoRetorno(prazoRetorno);
        } else {
            at.setDataEncerramento(LocalDateTime.now(ZoneSaoPaulo.ZONE));
            if (solucao != null) at.setSolucaoRegistrada(solucao);
        }

        registrarHistorico(agendamentoId, anterior.name(), novo.name(), justificativa, responsavelId);
        auditoria.registrar("ALTERAR", "atendimento", agendamentoId, responsavelId, request,
                "%s → %s".formatted(anterior, novo));
        return at;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('REGISTRAR_ATENDIMENTO')")
    public Atendimento buscar(Long agendamentoId) {
        return atendimentos.findById(agendamentoId)
                .orElseThrow(() -> new EntityNotFoundException("Atendimento não encontrado."));
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('REGISTRAR_ATENDIMENTO')")
    public List<HistoricoAtendimento> historico(Long agendamentoId) {
        return historico.findByAtendimentoIdOrderByDataRegistroAsc(agendamentoId);
    }

    /**
     * #N1c — confirmação dupla: o ALUNO confirma que o atendimento ocorreu.
     * Só permitido ao dono do agendamento; idempotente. O indicador de
     * "realizado" (EstatisticaService) só conta quando coordenador E aluno
     * confirmaram.
     */
    @Transactional
    @PreAuthorize("hasAuthority('AGENDAR_ATENDIMENTO')")
    public Atendimento confirmarComparecimento(Long agendamentoId, Long alunoId) {
        Atendimento at = atendimentos.findById(agendamentoId)
                .orElseThrow(() -> new EntityNotFoundException("Atendimento não encontrado."));
        // 404 neutro: não vazar atendimento de terceiro (RNF17/18)
        Agendamento ag = agendamentos.findById(agendamentoId).orElse(null);
        if (ag == null || !ag.getAlunoId().equals(alunoId)) {
            throw new EntityNotFoundException("Atendimento não encontrado.");
        }
        if (!at.isRealizado()) {
            throw new IllegalStateException(
                    "A coordenação ainda não registrou este atendimento.");
        }
        if (at.isConfirmadoAluno()) {
            return at; // idempotente
        }
        at.setConfirmadoAluno(true);
        at.setDataConfirmacaoAluno(LocalDateTime.now(ZoneSaoPaulo.ZONE));
        registrarHistorico(agendamentoId, null, at.getStatusResolucao().name(),
                "Comparecimento confirmado pelo aluno (#N1c)", alunoId);
        return at;
    }

    private void validarCamposPorStatus(StatusResolucao status, String solucao,
                                        String observacoes, LocalDate prazoRetorno) {
        switch (status) {
            case RESOLVIDO -> {
                if (solucao == null || solucao.isBlank()) {
                    throw new ValidacaoException("Status RESOLVIDO exige a solução registrada (RN07).");
                }
            }
            case NAO_RESOLVIDO -> {
                if (observacoes == null || observacoes.isBlank()) {
                    throw new ValidacaoException("Status NÃO RESOLVIDO exige justificativa/observação (RN09).");
                }
            }
            case EM_TRATATIVA -> {
                if (prazoRetorno == null) {
                    throw new ValidacaoException("Status EM TRATATIVA exige prazo de retorno (RN08).");
                }
            }
            default -> { /* AUSENCIA não passa por aqui */ }
        }
    }

    private void registrarHistorico(Long atendimentoId, String anterior, String novo,
                                    String descricao, Long usuarioId) {
        HistoricoAtendimento h = new HistoricoAtendimento();
        h.setAtendimentoId(atendimentoId);
        h.setStatusAnterior(anterior);
        h.setStatusNovo(novo);
        h.setDescricao(descricao);
        h.setUsuarioId(usuarioId);
        historico.save(h);   // append-only: nunca update/delete (RN13)
    }

    private StatusResolucao parseStatus(String s) {
        try {
            return StatusResolucao.valueOf(s);
        } catch (Exception e) {
            throw new ValidacaoException("Status inválido: " + s
                    + " (use RESOLVIDO, NAO_RESOLVIDO ou EM_TRATATIVA).");
        }
    }
}
