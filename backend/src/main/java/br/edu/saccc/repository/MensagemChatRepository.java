package br.edu.saccc.repository;

import br.edu.saccc.model.MensagemChat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MensagemChatRepository extends JpaRepository<MensagemChat, Long> {

    List<MensagemChat> findByAgendamentoIdOrderByDataEnvioAsc(Long agendamentoId);
}
