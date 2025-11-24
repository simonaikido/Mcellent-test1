package com.bim.seif.models;

import javax.persistence.*;
import lombok.Data;

@Data
@Entity
public class ConfiguracionOperacionMonetaria {

    @EmbeddedId
    private ConfiguracionOperacionMonetariaId configuracionOperacionId;

    @ManyToOne
    @MapsId("tipoOperacionMonetariaCve")
    @JoinColumn(name = "tipo_operacion_monetaria_cve")
    private TipoOperacionMonetaria tipoOperacionMonetaria;

    @ManyToOne
    @MapsId("tipoCampoCve")
    @JoinColumn(name = "tipo_campo_cve")
    private TipoCampo tipoCampo;

    private boolean requerido;

}