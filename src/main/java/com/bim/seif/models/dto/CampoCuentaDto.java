package com.bim.seif.models.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class CampoCuentaDto implements Serializable {
    private Long cuenta;
    private String banco;
    private DivisaDto divisa;
}
