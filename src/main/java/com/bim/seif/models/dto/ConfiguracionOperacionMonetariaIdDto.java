package com.bim.seif.models.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class ConfiguracionOperacionMonetariaIdDto implements Serializable {
    private final String tipoOperacionMonetariaCve;
    private final String tipoCampoCve;
}
