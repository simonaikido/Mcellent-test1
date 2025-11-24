package com.bim.seif.models;

import javax.persistence.*;

import lombok.Data;

@Data
@Entity
@Table(name = "configuracion_operacion_juridica")
public class ConfiguracionOperacionJuridica {

    @Id
    private String tipo_operacion_juridica_cve;

    @MapsId
    @ManyToOne
    @JoinColumn(name = "tipo_operacion_juridica_cve", referencedColumnName = "cve")
    private TipoOperacionJuridica tipoOperacionJuridica;

    private String rol;
    private boolean registro;
    private boolean revision;
    private boolean aprobacion;
}