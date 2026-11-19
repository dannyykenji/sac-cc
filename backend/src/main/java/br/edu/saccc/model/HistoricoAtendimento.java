package br.edu.saccc.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** Trilha append-only da demanda (classe 3.11, RN13): só INSERT, nunca UPDATE/DELETE. */
@Entity
@Table(name = "historico_atendimento")
public class HistoricoAtendimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "atendimento_id", nullable = false)
    private Long atendimentoId;

    @Column(name = "usuario_id", nullable = false)
    private Long usuarioId;

    @Column(name = "status_anterior", length = 15)
    private String statusAnterior;

    @Column(name = "status_novo", nullable = false, length = 15)
    private String statusNovo;

    @Column(length = 2000)
    private String descricao;

    @Column(name = "data_registro", nullable = false)
    private LocalDateTime dataRegistro;

    @PrePersist
    void aoCriar() {
        if (dataRegistro == null) dataRegistro = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getAtendimentoId() { return atendimentoId; }
    public void setAtendimentoId(Long atendimentoId) { this.atendimentoId = atendimentoId; }
    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }
    public String getStatusAnterior() { return statusAnterior; }
    public void setStatusAnterior(String statusAnterior) { this.statusAnterior = statusAnterior; }
    public String getStatusNovo() { return statusNovo; }
    public void setStatusNovo(String statusNovo) { this.statusNovo = statusNovo; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public LocalDateTime getDataRegistro() { return dataRegistro; }
}
