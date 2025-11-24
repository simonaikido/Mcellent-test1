package com.bim.seif.models.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class CampoCuentaCargoDto implements Serializable {
    private String cuenta;
    private String banco;
    private DivisaDto divisa;
}
