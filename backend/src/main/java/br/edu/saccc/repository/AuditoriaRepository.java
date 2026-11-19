package br.edu.saccc.repository;

import br.edu.saccc.model.Auditoria;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {

    Page<Auditoria> findByEntidadeOrUsuarioIdOrderByDataHoraDesc(String entidade, Long usuarioId, Pageable pageable);

    Page<Auditoria> findByEntidadeOrderByDataHoraDesc(String entidade, Pageable pageable);

    Page<Auditoria> findAllByOrderByDataHoraDesc(Pageable pageable);
}
