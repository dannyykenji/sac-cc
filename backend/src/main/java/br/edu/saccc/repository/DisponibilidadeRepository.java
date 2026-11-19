package br.edu.saccc.repository;

import br.edu.saccc.model.Disponibilidade;
import br.edu.saccc.model.StatusDisponibilidade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface DisponibilidadeRepository extends JpaRepository<Disponibilidade, Long> {

    List<Disponibilidade> findByDataBetweenOrderByDataAscHoraInicioAsc(LocalDate de, LocalDate ate);

    List<Disponibilidade> findByDataBetweenAndStatusOrderByDataAscHoraInicioAsc(
            LocalDate de, LocalDate ate, StatusDisponibilidade status);

    /** Conflito de intervalo na mesma data (RF13): sobreposição aberta clássica. */
    @Query("""
            select count(d) > 0 from Disponibilidade d
            where d.data = :data
              and d.horaInicio < :fim
              and :inicio < d.horaFim
            """)
    boolean existeConflito(@Param("data") LocalDate data,
                           @Param("inicio") LocalTime inicio,
                           @Param("fim") LocalTime fim);
}
