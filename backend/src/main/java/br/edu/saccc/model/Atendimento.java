package br.edu.saccc.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Execução e resultado de um atendimento (classe 3.9). PK = FK do agendamento (RN06, 1:1).
 * observacoes é INTERNO: nunca serializado para aluno (RF43).
 */
@Entity
@Table(name = "atendimento")
public class Atendimento {

    @Id
    @Column(name = "agendamento_id")
    private Long agendamentoId;

    @Column(name = "responsavel_id", nullable = false)
    private Long responsavelId;

    @Column(nullable = false)
    private boolean realizado;

    @Column(name = "data_realizacao", nullable = false)
    private LocalDateTime dataRealizacao;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_resolucao", nullable = false, length = 15)
    private StatusResolucao statusResolucao;

    @Column(length = 2000)
    private String observacoes;

    @Column(name = "solucao_registrada", length = 2000)
    private String solucaoRegistrada;

    @Column(length = 500)
    private String encaminhamento;

    @Column(name = "prazo_retorno")
    private LocalDate prazoRetorno;

    @Column(name = "data_encerramento")
    private LocalDateTime dataEncerramento;

    /** #N1c: confirmação dupla — aluno confirma o comparecimento. */
    @Column(name = "confirmado_aluno", nullable = false)
    private boolean confirmadoAluno;

    @Column(name = "data_confirmacao_aluno")
    private LocalDateTime dataConfirmacaoAluno;

    @PrePersist
    void aoCriar() {
        if (dataRealizacao == null) dataRealizacao = LocalDateTime.now();
    }

    public Long getAgendamentoId() { return agendamentoId; }
    public void setAgendamentoId(Long agendamentoId) { this.agendamentoId = agendamentoId; }
    public Long getResponsavelId() { return responsavelId; }
    public void setResponsavelId(Long responsavelId) { this.responsavelId = responsavelId; }
    public boolean isRealizado() { return realizado; }
    public void setRealizado(boolean realizado) { this.realizado = realizado; }
    public LocalDateTime getDataRealizacao() { return dataRealizacao; }
    public void setDataRealizacao(LocalDateTime dataRealizacao) { this.dataRealizacao = dataRealizacao; }
    public StatusResolucao getStatusResolucao() { return statusResolucao; }
    public void setStatusResolucao(StatusResolucao statusResolucao) { this.statusResolucao = statusResolucao; }
    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }
    public String getSolucaoRegistrada() { return solucaoRegistrada; }
    public void setSolucaoRegistrada(String solucaoRegistrada) { this.solucaoRegistrada = solucaoRegistrada; }
    public String getEncaminhamento() { return encaminhamento; }
    public void setEncaminhamento(String encaminhamento) { this.encaminhamento = encaminhamento; }
    public LocalDate getPrazoRetorno() { return prazoRetorno; }
    public void setPrazoRetorno(LocalDate prazoRetorno) { this.prazoRetorno = prazoRetorno; }
    public LocalDateTime getDataEncerramento() { return dataEncerramento; }
    public void setDataEncerramento(LocalDateTime dataEncerramento) { this.dataEncerramento = dataEncerramento; }
    public boolean isConfirmadoAluno() { return confirmadoAluno; }
    public void setConfirmadoAluno(boolean confirmadoAluno) { this.confirmadoAluno = confirmadoAluno; }
    public LocalDateTime getDataConfirmacaoAluno() { return dataConfirmacaoAluno; }
    public void setDataConfirmacaoAluno(LocalDateTime d) { this.dataConfirmacaoAluno = d; }
}
