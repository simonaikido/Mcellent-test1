package com.bim.seif.models.dto;

import lombok.Data;

@Data
public class VacacionesPrelogRequest {
    private String titularEmail;
    private String sustitutoEmail;

    // Por si quieres filtrar qué tipos pre-loggear (opcionales, default true):
    private Boolean incluirMonetarias = true;
    private Boolean incluirProgramadas = true;
    private Boolean incluirJuridicas = true;
    private Boolean applyNow;
}
