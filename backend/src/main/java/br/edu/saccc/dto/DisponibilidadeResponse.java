package br.edu.saccc.dto;

import br.edu.saccc.model.Disponibilidade;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalTime;

public record DisponibilidadeResponse(
        Long id,
        @JsonFormat(pattern = "yyyy-MM-dd") LocalDate data,
        @JsonFormat(pattern = "HH:mm") LocalTime horaInicio,
        @JsonFormat(pattern = "HH:mm") LocalTime horaFim,
        Short duracaoMinutos,
        String modalidade,
        String local,
        String linkReuniao,
        String status,
        String observacao) {

    public static DisponibilidadeResponse de(Disponibilidade d) {
        return new DisponibilidadeResponse(
                d.getId(), d.getData(), d.getHoraInicio(), d.getHoraFim(),
                d.getDuracaoMinutos(), d.getModalidade().name(), d.getLocal(),
                d.getLinkReuniao(), d.getStatus().name(), d.getObservacao());
    }
}
