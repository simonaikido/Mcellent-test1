package com.bim.seif.models.dto;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;

@Data
public class CrearAsignacionReq {

    @NotBlank @Email
    private String titularEmail;

    @NotBlank @Email
    private String sustitutoEmail;

    @NotNull
    private LocalDate inicio; // yyyy-MM-dd

    @NotNull
    private LocalDate fin;    // yyyy-MM-dd

    private Boolean autoRevert;        // default true
    private Boolean desactivarTitular; // solo guardamos el flag, la acción la haremos en fase 2
    private String motivo;
}