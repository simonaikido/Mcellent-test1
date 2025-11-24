package com.bim.seif.models.dto;

import lombok.Data;

@Data
public class SolicitudCuentaDto {
    //private String instruccionFolio; // Para buscar la Instruccion
    private Long cuenta;
    private String banco;
    private String divisaCve; // Clave de la divisa
    private String beneficiario;
    private String rfc;
    private String direccion;
    private String rutaEdoCta;
    private boolean solicitarEstadoCuenta;
    //private String fideicomiso;
}