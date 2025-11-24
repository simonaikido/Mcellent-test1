package com.bim.seif.models.dto;

import lombok.Data;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

@Data
public class FideicomisoDto implements Serializable {
    private final String folio;
    private final String alias;
    private final RegionDto region;
    private final boolean bloqueado;
    private final Set<FideicomisoContactoDto> clientes;
    private List<EjecutivoDto> ejecutivos;
    private final EstadoFideicomisoDto estado;
    Boolean instruccionesMonetarias;
    Boolean instruccionesJuridicas;
}