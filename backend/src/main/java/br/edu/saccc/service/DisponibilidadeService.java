package br.edu.saccc.service;

import br.edu.saccc.exception.ValidacaoException;
import br.edu.saccc.model.Disponibilidade;
import br.edu.saccc.model.ModalidadeAtendimento;
import br.edu.saccc.model.StatusDisponibilidade;
import br.edu.saccc.repository.DisponibilidadeRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Gerenciamento de disponibilidade (UC06, RF08–RF16).
 * A criação divide o intervalo [horaInicio, horaFim) em slots de `duracaoMinutos`,
 * cada um conflitando individualmente (RF13).
 */
@Service
public class DisponibilidadeService {

    private static final int MIN_DURACAO = 10;
    private static final int MAX_DURACAO = 240;
    private static final int MAX_SLOTS_POR_LOTE = 100;

    private final DisponibilidadeRepository repository;
    private final AuditoriaService auditoria;
    private final ParametroService parametros;

    public DisponibilidadeService(DisponibilidadeRepository repository,
                                  AuditoriaService auditoria,
                                  ParametroService parametros) {
        this.repository = repository;
        this.auditoria = auditoria;
        this.parametros = parametros;
    }

    @Transactional
    @PreAuthorize("hasAuthority('GERENCIAR_DISPONIBILIDADE')")
    public List<Disponibilidade> criarLote(Disponibilidade proto, Long criadoPor, HttpServletRequest request) {
        validar(proto);

        List<Disponibilidade> slots = new ArrayList<>();
        LocalTime inicio = proto.getHoraInicio();
        while (inicio.plusMinutes(proto.getDuracaoMinutos()).isBefore(proto.getHoraFim())
                || inicio.plusMinutes(proto.getDuracaoMinutos()).equals(proto.getHoraFim())) {
            LocalTime fim = inicio.plusMinutes(proto.getDuracaoMinutos());
            if (repository.existeConflito(proto.getData(), inicio, fim)) {
                throw new IllegalStateException(
                        "Conflito de horário: já existe disponibilidade entre %s e %s em %s."
                                .formatted(inicio, fim, proto.getData()));
            }
            Disponibilidade slot = new Disponibilidade();
            slot.setData(proto.getData());
            slot.setHoraInicio(inicio);
            slot.setHoraFim(fim);
            slot.setDuracaoMinutos(proto.getDuracaoMinutos());
            slot.setModalidade(proto.getModalidade());
            slot.setLocal(proto.getLocal());
            slot.setLinkReuniao(proto.getLinkReuniao());
            slot.setObservacao(proto.getObservacao());
            slot.setCriadoPorId(criadoPor);
            slots.add(slot);
            inicio = fim;
            if (slots.size() >= MAX_SLOTS_POR_LOTE) {
                throw new ValidacaoException("Lote excede o máximo de %d horários.".formatted(MAX_SLOTS_POR_LOTE));
            }
        }
        repository.saveAll(slots);
        auditoria.registrar("CRIAR", "disponibilidade", slots.get(0).getId(), criadoPor, request,
                "lote de %d horário(s) em %s".formatted(slots.size(), proto.getData()));
        return slots;
    }

    @Transactional
    @PreAuthorize("hasAuthority('GERENCIAR_DISPONIBILIDADE')")
    public void bloquear(Long id, Long usuarioId, HttpServletRequest request) {
        Disponibilidade d = buscar(id);
        if (d.getStatus() == StatusDisponibilidade.RESERVADO) {
            throw new IllegalStateException("Horário possui agendamento ativo e não pode ser bloqueado.");
        }
        d.setStatus(StatusDisponibilidade.BLOQUEADO);
        auditoria.registrar("BLOQUEAR", "disponibilidade", id, usuarioId, request, null);
    }

    @Transactional
    @PreAuthorize("hasAuthority('GERENCIAR_DISPONIBILIDADE')")
    public void desbloquear(Long id, Long usuarioId, HttpServletRequest request) {
        Disponibilidade d = buscar(id);
        d.setStatus(StatusDisponibilidade.DISPONIVEL);
        auditoria.registrar("DESBLOQUEAR", "disponibilidade", id, usuarioId, request, null);
    }

    /** Agenda para o ALUNO: apenas DISPONIVEL e datas futuras (RF15). */
    @Transactional(readOnly = true)
    public List<Disponibilidade> listarParaAluno(LocalDate de, LocalDate ate) {
        LocalDate inicio = de != null ? de : LocalDate.now(ZoneSaoPaulo.ZONE);
        LocalDate fim = ate != null ? ate : inicio.plusWeeks(4);
        return repository.findByDataBetweenAndStatusOrderByDataAscHoraInicioAsc(
                inicio, fim, StatusDisponibilidade.DISPONIVEL);
    }

    /** Agenda administrativa (RF14): por período, com todos os status. */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('GERENCIAR_DISPONIBILIDADE')")
    public List<Disponibilidade> listarAdmin(LocalDate de, LocalDate ate) {
        LocalDate inicio = de != null ? de : LocalDate.now(ZoneSaoPaulo.ZONE);
        LocalDate fim = ate != null ? ate : inicio.plusWeeks(2);
        return repository.findByDataBetweenOrderByDataAscHoraInicioAsc(inicio, fim);
    }

    @Transactional(readOnly = true)
    public Disponibilidade buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Horário não encontrado."));
    }

    private void validar(Disponibilidade d) {
        if (d.getData() == null || d.getHoraInicio() == null || d.getHoraFim() == null) {
            throw new ValidacaoException("Data, hora de início e hora de fim são obrigatórios.");
        }
        if (!d.getHoraFim().isAfter(d.getHoraInicio())) {
            throw new ValidacaoException("A hora de fim deve ser depois da hora de início.");
        }
        if (d.getDuracaoMinutos() == null || d.getDuracaoMinutos() < MIN_DURACAO || d.getDuracaoMinutos() > MAX_DURACAO) {
            throw new ValidacaoException("Duração deve estar entre %d e %d minutos.".formatted(MIN_DURACAO, MAX_DURACAO));
        }
        if (d.getModalidade() == null) {
            throw new ValidacaoException("Modalidade é obrigatória (ONLINE, PRESENCIAL ou HIBRIDO).");
        }
        if (d.getData().isBefore(LocalDate.now(ZoneSaoPaulo.ZONE))) {
            throw new ValidacaoException("Não é possível criar disponibilidade em data passada.");
        }
        // mesmo dia: hora de início precisa ser futura (bug real — permitia
        // abrir atendimento das 14h quando já eram 16h)
        if (d.getData().equals(LocalDate.now(ZoneSaoPaulo.ZONE))
                && !d.getHoraInicio().isAfter(java.time.LocalTime.now(ZoneSaoPaulo.ZONE))) {
            throw new ValidacaoException(
                    "Não é possível criar disponibilidade com hora de início já passada (agora são %s)."
                            .formatted(java.time.LocalTime.now(ZoneSaoPaulo.ZONE)));
        }
        if (d.getModalidade() == ModalidadeAtendimento.PRESENCIAL
                && (d.getLocal() == null || d.getLocal().isBlank())) {
            // #N7 (coordenação 2026-11-19): local fixo configurável — usado quando
            // a coordenação não informa um local específico no horário
            d.setLocal(parametros.localPresencialFixo());
        }
        // #N6 (coordenação 2026-11-19): online é sala automática (o próprio chat
        // do sistema, ADR-008) — link externo deixou de ser usado
        if (d.getLinkReuniao() != null && d.getLinkReuniao().isBlank()) {
            throw new ValidacaoException("Link da reunião não pode ficar em branco.");
        }
    }
}
