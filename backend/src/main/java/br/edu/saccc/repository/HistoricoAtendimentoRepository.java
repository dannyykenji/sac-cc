package br.edu.saccc.repository;

import br.edu.saccc.model.HistoricoAtendimento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistoricoAtendimentoRepository extends JpaRepository<HistoricoAtendimento, Long> {

    List<HistoricoAtendimento> findByAtendimentoIdOrderByDataRegistroAsc(Long atendimentoId);
}
