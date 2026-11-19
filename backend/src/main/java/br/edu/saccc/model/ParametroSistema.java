package br.edu.saccc.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Parâmetros do sistema (RNF42) — regras #N3/#N4/#N5 parametrizáveis. */
@Entity
@Table(name = "parametro_sistema")
public class ParametroSistema {

    @Id
    @Column(length = 50)
    private String chave;

    @Column(nullable = false, length = 100)
    private String valor;

    @Column(length = 255)
    private String descricao;

    public String getChave() { return chave; }
    public void setChave(String chave) { this.chave = chave; }
    public String getValor() { return valor; }
    public void setValor(String valor) { this.valor = valor; }
    public String getDescricao() { return descricao; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
}
