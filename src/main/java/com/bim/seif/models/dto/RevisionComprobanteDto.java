package com.bim.seif.models.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class RevisionComprobanteDto implements Serializable {

    private String observacion;
    private Boolean aprobada;
}
