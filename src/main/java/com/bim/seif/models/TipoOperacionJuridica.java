package com.bim.seif.models;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Data;

@Data
@Entity
@Table(name = "TIPO_OPERACION_JURIDICA")
public class TipoOperacionJuridica {

    @Id
    private String cve;
    private String descripcion;
    private boolean estado;
    //private boolean validacionGeneral;
}