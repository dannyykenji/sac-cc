package br.edu.saccc.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;

/** Usuário com permissões administrativas (classe 3.4). */
@Entity
@Table(name = "administrador")
@PrimaryKeyJoinColumn(name = "usuario_id")
public class Administrador extends Usuario {

    @Column(name = "nivel_administrativo", nullable = false, length = 30)
    private String nivelAdministrativo = "PADRAO";

    public String getNivelAdministrativo() { return nivelAdministrativo; }
    public void setNivelAdministrativo(String nivelAdministrativo) { this.nivelAdministrativo = nivelAdministrativo; }
}
