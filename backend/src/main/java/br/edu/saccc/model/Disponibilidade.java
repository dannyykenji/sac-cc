package br.edu.saccc.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Período disponibilizado para agendamento (classe 3.7).
 * Cada instância representa UM slot de `duracaoMinutos` (a criação em lote
 * divide o intervalo informado em N slots — UC06).
 */
@Entity
@Table(name = "disponibilidade")
public class Disponibilidade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "data", nullable = false)
    private LocalDate data;

    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "hora_fim", nullable = false)
    private LocalTime horaFim;

    @Column(name = "duracao_minutos", nullable = false)
    private Short duracaoMinutos;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ModalidadeAtendimento modalidade;

    @Column(length = 255)
    private String local;

    @Column(name = "link_reuniao", length = 500)
    private String linkReuniao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private StatusDisponibilidade status = StatusDisponibilidade.DISPONIVEL;

    @Column(length = 500)
    private String observacao;

    /** FK para usuario(id) — guardado como id simples para evitar joins (RNF14). */
    @Column(name = "criado_por", nullable = false)
    private Long criadoPorId;

    @Column(name = "data_criacao", nullable = false)
    private LocalDateTime dataCriacao;

    @PrePersist
    void aoCriar() {
        if (dataCriacao == null) dataCriacao = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public LocalDate getData() { return data; }
    public void setData(LocalDate data) { this.data = data; }
    public LocalTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalTime horaInicio) { this.horaInicio = horaInicio; }
    public LocalTime getHoraFim() { return horaFim; }
    public void setHoraFim(LocalTime horaFim) { this.horaFim = horaFim; }
    public Short getDuracaoMinutos() { return duracaoMinutos; }
    public void setDuracaoMinutos(Short duracaoMinutos) { this.duracaoMinutos = duracaoMinutos; }
    public ModalidadeAtendimento getModalidade() { return modalidade; }
    public void setModalidade(ModalidadeAtendimento modalidade) { this.modalidade = modalidade; }
    public String getLocal() { return local; }
    public void setLocal(String local) { this.local = local; }
    public String getLinkReuniao() { return linkReuniao; }
    public void setLinkReuniao(String linkReuniao) { this.linkReuniao = linkReuniao; }
    public StatusDisponibilidade getStatus() { return status; }
    public void setStatus(StatusDisponibilidade status) { this.status = status; }
    public String getObservacao() { return observacao; }
    public void setObservacao(String observacao) { this.observacao = observacao; }
    public Long getCriadoPorId() { return criadoPorId; }
    public void setCriadoPorId(Long criadoPorId) { this.criadoPorId = criadoPorId; }
    public LocalDateTime getDataCriacao() { return dataCriacao; }
}
