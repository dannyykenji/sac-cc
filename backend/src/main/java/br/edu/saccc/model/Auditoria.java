package br.edu.saccc.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/** Trilha de auditoria de operações relevantes (classe 3.14, RF61–RF64). */
@Entity
@Table(name = "auditoria")
public class Auditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** null para operações anônimas (ex.: auto-cadastro, tentativa de login). */
    @Column(name = "usuario_id")
    private Long usuarioId;

    /** CRIAR | ALTERAR | DESATIVAR | APROVAR | REJEITAR | CANCELAR | ... */
    @Column(nullable = false, length = 50)
    private String operacao;

    /** 'usuario', 'agendamento', ... */
    @Column(nullable = false, length = 50)
    private String entidade;

    @Column(name = "identificador_entidade")
    private Long identificadorEntidade;

    @Column(name = "data_hora", nullable = false)
    private Instant dataHora = Instant.now();

    @Column(name = "endereco_ip", length = 64)
    private String enderecoIp;

    /** Sem dados sensíveis (RNF25). */
    @Column(length = 1000)
    private String detalhes;

    public Long getId() { return id; }
    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }
    public String getOperacao() { return operacao; }
    public void setOperacao(String operacao) { this.operacao = operacao; }
    public String getEntidade() { return entidade; }
    public void setEntidade(String entidade) { this.entidade = entidade; }
    public Long getIdentificadorEntidade() { return identificadorEntidade; }
    public void setIdentificadorEntidade(Long identificadorEntidade) { this.identificadorEntidade = identificadorEntidade; }
    public Instant getDataHora() { return dataHora; }
    public String getEnderecoIp() { return enderecoIp; }
    public void setEnderecoIp(String enderecoIp) { this.enderecoIp = enderecoIp; }
    public String getDetalhes() { return detalhes; }
    public void setDetalhes(String detalhes) { this.detalhes = detalhes; }
}
