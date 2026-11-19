package br.edu.saccc.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

/** UC08 — registro de realização/ausência com campos condicionais (RN07–RN09, RN12). */
public record RegistrarAtendimentoRequest(
        @NotNull Long agendamentoId,
        @NotNull Boolean realizado,
        /** obrigatório se realizado=true; proibido se false (RN12) */
        String statusResolucao,
        /** interno; obrigatório se NAO_RESOLVIDO (RN09) */
        String observacoes,
        /** obrigatório se RESOLVIDO (RN07) */
        String solucao,
        String encaminhamento,
        /** obrigatório se EM_TRATATIVA (RN08) — yyyy-MM-dd */
        LocalDate prazoRetorno) {
}
