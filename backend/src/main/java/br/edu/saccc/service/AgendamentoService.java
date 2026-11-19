package br.edu.saccc.service;

import br.edu.saccc.exception.ValidacaoException;
import br.edu.saccc.model.Agendamento;
import br.edu.saccc.model.Disponibilidade;
import br.edu.saccc.model.ModalidadeAtendimento;
import br.edu.saccc.model.StatusAgendamento;
import br.edu.saccc.model.StatusDisponibilidade;
import br.edu.saccc.model.TipoNotificacao;
import br.edu.saccc.repository.AgendamentoRepository;
import br.edu.saccc.repository.DisponibilidadeLockRepository;
import br.edu.saccc.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Agendamento (UC03–UC05, RF17–RF30, RN01–RN05, RN15).
 * Anti-duplicação em 3 camadas (ADR-005): validação + LOCK pessimista na transação
 * + índice único parcial no PostgreSQL (última defesa, independente da aplicação).
 */
@Service
public class AgendamentoService {

    // fuso configurável (RNF62): ZoneSaoPaulo.ZONE lê SACCC_TIMEZONE
    private static final SecureRandom RANDOM = new SecureRandom();

    private final AgendamentoRepository agendamentos;
    private final DisponibilidadeLockRepository dispLock;
    private final UsuarioRepository usuarioRepository;
    private final ParametroService parametros;
    private final AuditoriaService auditoria;
    private final NotificacaoService notificacoes;

    public AgendamentoService(AgendamentoRepository agendamentos,
                              DisponibilidadeLockRepository dispLock,
                              UsuarioRepository usuarioRepository,
                              ParametroService parametros,
                              AuditoriaService auditoria,
                              NotificacaoService notificacoes) {
        this.agendamentos = agendamentos;
        this.dispLock = dispLock;
        this.usuarioRepository = usuarioRepository;
        this.parametros = parametros;
        this.auditoria = auditoria;
        this.notificacoes = notificacoes;
    }

    /** UC03 — confirmação transacional com revalidação completa do slot. */
    @Transactional
    @PreAuthorize("hasAuthority('AGENDAR_ATENDIMENTO')")
    public Agendamento agendar(Long alunoId, Long disponibilidadeId, String modalidadeStr,
                               Long categoriaId, String assunto, String descricao,
                               HttpServletRequest request, Long usuarioIdAudit) {
        if (assunto == null || assunto.isBlank()) {
            throw new ValidacaoException("O assunto do atendimento é obrigatório (RF19).");
        }

        // LOCK NA LINHA DO ALUNO (ADR-005): serializa agendamentos concorrentes do
        // MESMO aluno (RN03/#N4 são leituras-then-insert sem esse lock). Alunos
        // diferentes travam linhas diferentes — sem contenção entre eles.
        if (usuarioRepository.travarLinha(alunoId) == null) {
            throw new jakarta.persistence.EntityNotFoundException("Aluno não encontrado.");
        }

        // LOCK pessimista no slot (ADR-005 camada 2): dentro da transação,
        // duas confirmações concorrentes se serializam aqui.
        Disponibilidade slot = dispLock.buscarComLock(disponibilidadeId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Horário não encontrado."));

        // revalidação completa dentro da transação (UC03: "verifica novamente a disponibilidade")
        if (slot.getStatus() != StatusDisponibilidade.DISPONIVEL) {
            throw new IllegalStateException("Este horário acabou de ser ocupado ou bloqueado. Escolha outro (409).");
        }

        ModalidadeAtendimento modalidade = ModalidadeAtendimento.valueOf(modalidadeStr);
        // RN15: modalidade compatível com o slot (HIBRIDO aceita ONLINE ou PRESENCIAL)
        if (!modalidadePermitida(slot.getModalidade(), modalidade)) {
            throw new ValidacaoException(
                    "Modalidade %s não é permitida para este horário (permitida: %s)."
                            .formatted(modalidade, slot.getModalidade()));
        }

        LocalDateTime agora = LocalDateTime.now(ZoneSaoPaulo.ZONE);
        LocalDateTime inicioSlot = LocalDateTime.of(slot.getData(), slot.getHoraInicio());
        if (inicioSlot.isBefore(agora.plusHours(parametros.antecedenciaMinimaHoras()))) {
            throw new ValidacaoException(
                    "Agendamento exige antecedência mínima de %d horas (#N5, parametrizável)."
                            .formatted(parametros.antecedenciaMinimaHoras()));
        }

        // RN03: aluno não pode ter sobreposição com agendamento ativo próprio
        if (agendamentos.existeSobreposicao(alunoId, slot.getData(), slot.getHoraInicio(), slot.getHoraFim())) {
            throw new IllegalStateException("Você já possui agendamento neste período (RN03).");
        }

        // #N4: limite de agendamentos futuros ativos
        long ativos = agendamentos.countAtivosFuturos(alunoId, LocalDate.now(ZoneSaoPaulo.ZONE));
        if (ativos >= parametros.limiteAgendamentosFuturos()) {
            throw new IllegalStateException(
                    "Limite de %d agendamento(s) futuro(s) ativo(s) atingido (#N4). Cancele ou aguarde."
                            .formatted(parametros.limiteAgendamentosFuturos()));
        }

        Agendamento a = new Agendamento();
        a.setAlunoId(alunoId);
        a.setDisponibilidadeId(slot.getId());
        a.setCategoriaId(categoriaId);
        a.setModalidade(modalidade);
        a.setAssunto(assunto.trim());
        a.setDescricao(descricao);
        a.setData(slot.getData());
        a.setHoraInicio(slot.getHoraInicio());
        a.setHoraFim(slot.getHoraFim());
        a.setProtocolo(gerarProtocolo());
        a.setStatus(StatusAgendamento.SOLICITADO);

        slot.setStatus(StatusDisponibilidade.RESERVADO); // RF16: slot sai da disponibilidade
        Agendamento salvo = agendamentos.save(a);

        // RF57: coordenação é notificada de novo agendamento
        notificacoes.notificar(slot.getCriadoPorId(), salvo.getId(), TipoNotificacao.CONFIRMACAO,
                "Novo agendamento",
                "Aluno agendou atendimento em %s às %s (protocolo %s)."
                        .formatted(salvo.getData(), salvo.getHoraInicio(), salvo.getProtocolo()));

        auditoria.registrar("CRIAR", "agendamento", salvo.getId(), usuarioIdAudit, request,
                "protocolo %s".formatted(salvo.getProtocolo()));
        return salvo;
    }

    /** UC05 — cancelamento pelo aluno (respeitando prazo #N3) ou coordenação. */
    @Transactional
    @PreAuthorize("hasAuthority('CANCELAR_AGENDAMENTO') or hasAuthority('GERENCIAR_DISPONIBILIDADE')")
    public Agendamento cancelar(Long agendamentoId, Long usuarioId, boolean pelaCoordenacao,
                                String motivo, HttpServletRequest request) {
        Agendamento a = agendamentos.findById(agendamentoId)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Agendamento não encontrado."));

        if (a.getStatus() != StatusAgendamento.SOLICITADO && a.getStatus() != StatusAgendamento.CONFIRMADO) {
            throw new IllegalStateException("Somente agendamentos ativos podem ser cancelados.");
        }

        if (!pelaCoordenacao) {
            // aluno: prazo mínimo parametrizável (#N3); motivo obrigatório (RF28)
            if (motivo == null || motivo.isBlank()) {
                throw new ValidacaoException("Informe o motivo do cancelamento (RF28).");
            }
            LocalDateTime limite = LocalDateTime.of(a.getData(), a.getHoraInicio())
                    .minusHours(parametros.prazoCancelamentoHoras());
            if (LocalDateTime.now(ZoneSaoPaulo.ZONE).isAfter(limite)) {
                throw new IllegalStateException(
                        "Prazo para cancelamento expirou (mínimo %d horas antes; #N3). Procure a coordenação."
                                .formatted(parametros.prazoCancelamentoHoras()));
            }
        } else if (motivo == null || motivo.isBlank()) {
            throw new ValidacaoException("Coordenação deve informar motivo obrigatório ao cancelar (UC05).");
        }

        a.setStatus(StatusAgendamento.CANCELADO);
        a.setDataCancelamento(LocalDateTime.now(ZoneSaoPaulo.ZONE));
        a.setMotivoCancelamento(motivo);
        a.setCanceladoPorId(usuarioId);

        // libera o slot (regra default #N3: volta a ficar disponível)
        Disponibilidade slot = dispLock.findById(a.getDisponibilidadeId()).orElse(null);
        if (slot != null) {
            slot.setStatus(StatusDisponibilidade.DISPONIVEL);

            // RF57/58: notifica a outra parte do cancelamento
            if (pelaCoordenacao) {
                notificacoes.notificar(a.getAlunoId(), a.getId(), TipoNotificacao.STATUS,
                        "Atendimento cancelado pela coordenação",
                        "Seu atendimento de %s às %s foi cancelado. Motivo: %s"
                                .formatted(a.getData(), a.getHoraInicio(), motivo));
            } else {
                notificacoes.notificar(slot.getCriadoPorId(), a.getId(), TipoNotificacao.STATUS,
                        "Atendimento cancelado pelo aluno",
                        "O atendimento de %s às %s (protocolo %s) foi cancelado pelo aluno. Motivo: %s"
                                .formatted(a.getData(), a.getHoraInicio(), a.getProtocolo(), motivo));
            }
        }

        auditoria.registrar("CANCELAR", "agendamento", a.getId(), usuarioId, request,
                "protocolo %s — %s".formatted(a.getProtocolo(), motivo));
        return a;
    }

    /** UC04 — próximos (ativos) do aluno. */
    @Transactional(readOnly = true)
    public List<Agendamento> proximosDoAluno(Long alunoId) {
        return agendamentos.findByAlunoIdAndStatusInOrderByDataAscHoraInicioAsc(
                alunoId, List.of(StatusAgendamento.SOLICITADO, StatusAgendamento.CONFIRMADO));
    }

    /** UC04 — histórico do aluno. */
    @Transactional(readOnly = true)
    public List<Agendamento> historicoDoAluno(Long alunoId) {
        return agendamentos.findByAlunoIdAndStatusNotInOrderByDataDescHoraInicioDesc(
                alunoId, List.of(StatusAgendamento.SOLICITADO, StatusAgendamento.CONFIRMADO));
    }

    @Transactional(readOnly = true)
    public Agendamento buscar(Long id) {
        return agendamentos.findById(id)
                .orElseThrow(() -> new jakarta.persistence.EntityNotFoundException("Agendamento não encontrado."));
    }

    @Transactional(readOnly = true)
    public boolean pertenceAoAluno(Long agendamentoId, Long alunoId) {
        return agendamentos.findById(agendamentoId)
                .map(a -> a.getAlunoId().equals(alunoId))
                .orElse(false);
    }

    /** RN15: ONLINE→ONLINE; PRESENCIAL→PRESENCIAL; HIBRIDO→ambos. */
    static boolean modalidadePermitida(ModalidadeAtendimento doSlot, ModalidadeAtendimento escolhida) {
        if (doSlot == ModalidadeAtendimento.HIBRIDO) {
            return escolhida == ModalidadeAtendimento.ONLINE || escolhida == ModalidadeAtendimento.PRESENCIAL;
        }
        return doSlot == escolhida;
    }

    /** Protocolo AG-AAAA-NNNNNN (RN04: único; coluna UNIQUE + geração aleatória). */
    private String gerarProtocolo() {
        String ano = String.valueOf(LocalDate.now(ZoneSaoPaulo.ZONE).getYear());
        for (int tentativa = 0; tentativa < 10; tentativa++) {
            String proto = "AG-" + ano + "-" + String.format("%06d", RANDOM.nextInt(1_000_000));
            if (!agendamentos.existsByProtocolo(proto)) {
                return proto;
            }
        }
        throw new IllegalStateException("Não foi possível gerar protocolo único.");
    }
}
