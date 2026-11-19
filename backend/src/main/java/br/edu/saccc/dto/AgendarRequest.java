package br.edu.saccc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AgendarRequest(
        @NotNull Long disponibilidadeId,
        @NotNull String modalidade,           // ONLINE | PRESENCIAL (slot HIBRIDO aceita ambos)
        Long categoriaId,
        @NotBlank @Size(max = 255) String assunto,
        @Size(max = 2000) String descricao) {
}
