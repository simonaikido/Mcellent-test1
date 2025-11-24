package com.bim.seif.models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "TIPO_OPERACION_MONETARIA")
public class TipoOperacionMonetaria {

    @Id
    private String cve;
    @Column(nullable = false)
    private String descripcion;
    @Column(nullable = false)
    private boolean programacion;
    @Column(nullable = false)
    private boolean validacionMesaControl;
    @Column(nullable = false)
    private boolean validacionGeneral;
    @Column(nullable = false)
    private boolean validacionMontos;
}
