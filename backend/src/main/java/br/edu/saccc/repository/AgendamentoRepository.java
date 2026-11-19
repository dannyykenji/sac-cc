package br.edu.saccc.repository;

import br.edu.saccc.model.Agendamento;
import br.edu.saccc.model.StatusAgendamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;

public interface AgendamentoRepository extends JpaRepository<Agendamento, Long>,
        org.springframework.data.jpa.repository.JpaSpecificationExecutor<Agendamento> {

    List<Agendamento> findByAlunoIdAndStatusInOrderByDataAscHoraInicioAsc(Long alunoId, List<StatusAgendamento> statuses);

    List<Agendamento> findByAlunoIdAndStatusNotInOrderByDataDescHoraInicioDesc(Long alunoId, List<StatusAgendamento> statuses);

    /** #N9: agendamentos ONLINE ativos começando dentro da janela de lembrete. */
    List<Agendamento> findByStatusInAndModalidadeAndDataAndHoraInicioBetween(
            List<StatusAgendamento> statuses, br.edu.saccc.model.ModalidadeAtendimento modalidade,
            java.time.LocalDate data, java.time.LocalTime de, java.time.LocalTime ate);

    boolean existsByProtocolo(String protocolo);

    /** #N4: agendamentos ativos futuros do aluno. */
    @Query("""
            select count(a) from Agendamento a
            where a.alunoId = :alunoId
              and a.status in (br.edu.saccc.model.StatusAgendamento.SOLICITADO,
                               br.edu.saccc.model.StatusAgendamento.CONFIRMADO)
              and a.data >= :hoje
            """)
    long countAtivosFuturos(@Param("alunoId") Long alunoId, @Param("hoje") LocalDate hoje);

    /** RN03: sobreposição com agendamento ativo do próprio aluno no mesmo dia. */
    @Query("""
            select count(a) > 0 from Agendamento a
            where a.alunoId = :alunoId
              and a.status in (br.edu.saccc.model.StatusAgendamento.SOLICITADO,
                               br.edu.saccc.model.StatusAgendamento.CONFIRMADO)
              and a.data = :data
              and a.horaInicio < :fim
              and :inicio < a.horaFim
            """)
    boolean existeSobreposicao(@Param("alunoId") Long alunoId, @Param("data") LocalDate data,
                               @Param("inicio") java.time.LocalTime inicio,
                               @Param("fim") java.time.LocalTime fim);

    /** UC07/RF42: agenda administrativa via Specification no AgendaAdminController
     *  (JPQL com "(:x is null or ...)" quebrava no PG com parâmetro nulo — E2E 2026-11-05). */
}
