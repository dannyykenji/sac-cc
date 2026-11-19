package br.edu.saccc.repository;

import br.edu.saccc.model.Aluno;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlunoRepository extends JpaRepository<Aluno, Long> {

    boolean existsByMatricula(String matricula);

    Page<Aluno> findByStatus(br.edu.saccc.model.StatusUsuario status, Pageable pageable);
}
