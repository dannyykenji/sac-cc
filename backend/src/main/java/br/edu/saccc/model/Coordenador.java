package br.edu.saccc.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

/** Responsável pela agenda e pelos atendimentos (classe 3.3). */
@Entity
@Table(name = "coordenador")
@PrimaryKeyJoinColumn(name = "usuario_id")
public class Coordenador extends Usuario {

    @Column(name = "registro_funcional", unique = true, length = 50)
    private String registroFuncional;

    @Column(length = 100)
    private String cargo;

    @Column(name = "area_responsabilidade", length = 255)
    private String areaResponsabilidade;

    public String getRegistroFuncional() { return registroFuncional; }
    public void setRegistroFuncional(String registroFuncional) { this.registroFuncional = registroFuncional; }
    public String getCargo() { return cargo; }
    public void setCargo(String cargo) { this.cargo = cargo; }
    public String getAreaResponsabilidade() { return areaResponsabilidade; }
    public void setAreaResponsabilidade(String areaResponsabilidade) { this.areaResponsabilidade = areaResponsabilidade; }
}
