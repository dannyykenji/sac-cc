package br.edu.saccc.dto;

import br.edu.saccc.model.Aluno;
import br.edu.saccc.model.Usuario;

import java.time.LocalDateTime;

public record UsuarioAdminResponse(
        Long id,
        String nome,
        String email,
        String perfil,
        String status,
        LocalDateTime dataCadastro,
        // dados de aluno (quando aplicável)
        String matricula,
        String curso,
        Short periodo,
        String turma,
        String telefone,
        String statusMatricula,
        // dados de coordenador (quando aplicável)
        String registroFuncional,
        String cargo) {

    public static UsuarioAdminResponse de(Usuario u) {
        Aluno aluno = u instanceof Aluno a ? a : null;
        br.edu.saccc.model.Coordenador coord = u instanceof br.edu.saccc.model.Coordenador c ? c : null;
        return new UsuarioAdminResponse(
                u.getId(), u.getNome(), u.getEmail(), u.getPerfil().getNome(),
                u.getStatus().name(), u.getDataCadastro(),
                aluno != null ? aluno.getMatricula() : null,
                aluno != null ? aluno.getCurso() : null,
                aluno != null ? aluno.getPeriodo() : null,
                aluno != null ? aluno.getTurma() : null,
                aluno != null ? aluno.getTelefone() : null,
                aluno != null ? aluno.getStatusMatricula() : null,
                coord != null ? coord.getRegistroFuncional() : null,
                coord != null ? coord.getCargo() : null);
    }
}
