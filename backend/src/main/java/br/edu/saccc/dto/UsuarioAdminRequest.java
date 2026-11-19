package br.edu.saccc.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Map;

/** Criação/edição de usuário pelo ADMIN (UC11). */
public record UsuarioAdminRequest(
        @NotBlank @Size(max = 255) String nome,
        @NotBlank @Email @Size(max = 255) String email,
        @NotNull @Pattern(regexp = "ALUNO|COORDENADOR|ADMIN", message = "Perfil inválido")
        String perfil,
        /** obrigatória apenas na criação */
        @Size(min = 8, max = 72)
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "A senha deve conter letras e números")
        String senha,
        /** campos opcionais por perfil: matricula, registroFuncional, cargo, turma... */
        Map<String, String> extras) {
}
