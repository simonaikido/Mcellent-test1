package com.bim.seif.models.dto;

import lombok.AllArgsConstructor;
import lombok.Data;


@Data
@AllArgsConstructor
public class SolicitudCuentaResumenDto {
    private String contacto;//0
    private String cuenta;//1
    private String banco;//2
    private String beneficiario;//3
    private String rfc;//4
    private String direccion;//5
    private String divisaCve;//6
    private boolean nacional;//7
    private Integer tipoCambio; //8
    private String descripcion;//9
}