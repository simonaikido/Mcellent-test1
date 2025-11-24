package com.bim.seif.models.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class ConfiguracionOperacionMonetariaDto implements Serializable {
    private final ConfiguracionOperacionMonetariaIdDto configuracionOperacionId;
    private final TipoOperacionMonetariaDto tipoOperacionMonetaria;
    private final TipoCampoDto tipoCampo;
    private final boolean requerido;
}
