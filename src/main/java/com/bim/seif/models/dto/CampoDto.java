package com.bim.seif.models.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
public class CampoDto implements Serializable {
//    private Long id;
    private OperacionMonetariaDto operacionMonetaria;
    private TipoCampoDto tipoCampo;
}
