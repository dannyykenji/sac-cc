package br.edu.saccc.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * Criação em lote (UC06): data + intervalo + duração → N slots.
 * Validações de negócio (conflito, data passada, local presencial) ficam no serviço.
 */
public record CriarDisponibilidadeRequest(
        @NotNull LocalDate data,
        @NotNull LocalTime horaInicio,
        @NotNull LocalTime horaFim,
        @NotNull Short duracaoMinutos,
        @NotNull String modalidade,
        String local,
        String linkReuniao,
        String observacao) {
}
