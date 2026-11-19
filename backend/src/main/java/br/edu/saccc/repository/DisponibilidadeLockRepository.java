package br.edu.saccc.repository;

import br.edu.saccc.model.Disponibilidade;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface DisponibilidadeLockRepository extends JpaRepository<Disponibilidade, Long> {

    /** ADR-005 camada 2: lock pessimista na confirmação do agendamento (RNF11). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Disponibilidade d where d.id = :id")
    Optional<Disponibilidade> buscarComLock(@Param("id") Long id);
}
