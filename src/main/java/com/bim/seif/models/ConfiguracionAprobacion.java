package com.bim.seif.models;


import javax.persistence.*;
import lombok.Data;

@Data
@Entity
public class ConfiguracionAprobacion {

    @Id
    private String cve;

    private boolean general;
    private boolean monto;
    // observaciones de mesa de control
    private boolean observaciones;

    @ManyToOne
    @JoinColumn(name = "tipo_operacion_cve")
    private TipoOperacionMonetaria tipoOperacion;



}
