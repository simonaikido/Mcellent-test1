package com.bim.seif.models.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class RegionDto implements Serializable {
    private String cve;
    private String descripcion;
}
