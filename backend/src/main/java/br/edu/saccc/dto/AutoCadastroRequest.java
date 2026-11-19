package br.edu.saccc.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Auto-cadastro público do aluno (ADR-006). Política de senha: 8+ caracteres,
 * ao menos uma letra e um número (pendente de confirmação — ver API_FASE1.md).
 */
public record AutoCadastroRequest(
        @NotBlank @Size(max = 255) String nome,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(max = 50) @Pattern(regexp = "^[0-9]+$", message = "Matrícula deve conter apenas números")
        String matricula,
        @NotBlank @Size(min = 8, max = 72)
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "A senha deve conter letras e números")
        String senha,
        Short periodo,
        @Size(max = 50) String turma,
        @Size(max = 30) String telefone) {
}
