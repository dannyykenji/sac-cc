package br.edu.saccc.repository;

import br.edu.saccc.model.Notificacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {

    List<Notificacao> findByDestinatarioIdOrderByDataCriacaoDesc(Long destinatarioId);
}
