package br.edu.saccc.dto;

import br.edu.saccc.model.Agendamento;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** Visão do agendamento para o ALUNO — sem observações internas (RF43). */
public record AgendamentoResponse(
        Long id,
        String protocolo,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate data,
        @JsonFormat(pattern = "HH:mm") LocalTime horaInicio,
        @JsonFormat(pattern = "HH:mm") LocalTime horaFim,
        String modalidade,
        Long categoriaId,
        String assunto,
        String descricao,
        String status,
        LocalDateTime dataCancelamento,
        String motivoCancelamento) {

    public static AgendamentoResponse de(Agendamento a) {
        return new AgendamentoResponse(
                a.getId(), a.getProtocolo(), a.getData(), a.getHoraInicio(), a.getHoraFim(),
                a.getModalidade().name(), a.getCategoriaId(), a.getAssunto(), a.getDescricao(),
                a.getStatus().name(), a.getDataCancelamento(), a.getMotivoCancelamento());
    }
}
