package com.bim.seif.models.dto;

import lombok.Data;

@Data
public class ClienteFideicomisoDto {
    //datos fideicomiso
    private final String folio;
    private final String alias;
    private final String tipo;
    private final String subtipo;
    private final boolean bloqueado;

    // datos del cliente
    private final String nombre;
    private final String telefono;
    private final String email;
    private final String facultad;
}

