package br.edu.saccc.repository;

import br.edu.saccc.model.CategoriaAtendimento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CategoriaAtendimentoRepository extends JpaRepository<CategoriaAtendimento, Long> {

    List<CategoriaAtendimento> findByAtivoTrueOrderByNome();
}
