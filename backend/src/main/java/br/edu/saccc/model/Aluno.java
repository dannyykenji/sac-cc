package br.edu.saccc.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

/** Estudante que solicita atendimento (classe 3.2). */
@Entity
@Table(name = "aluno")
@PrimaryKeyJoinColumn(name = "usuario_id")
public class Aluno extends Usuario {

    @Column(nullable = false, unique = true, length = 50)
    private String matricula;

    @Column(nullable = false, length = 100)
    private String curso = "Ciência da Computação";

    @Column(columnDefinition = "SMALLINT")
    private Short periodo;

    @Column(length = 50)
    private String turma;

    @Column(length = 30)
    private String telefone;

    @Column(name = "status_matricula", nullable = false, length = 30)
    private String statusMatricula = "ATIVA";

    public String getMatricula() { return matricula; }
    public void setMatricula(String matricula) { this.matricula = matricula; }
    public String getCurso() { return curso; }
    public void setCurso(String curso) { this.curso = curso; }
    public Short getPeriodo() { return periodo; }
    public void setPeriodo(Short periodo) { this.periodo = periodo; }
    public String getTurma() { return turma; }
    public void setTurma(String turma) { this.turma = turma; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public String getStatusMatricula() { return statusMatricula; }
    public void setStatusMatricula(String statusMatricula) { this.statusMatricula = statusMatricula; }
}
