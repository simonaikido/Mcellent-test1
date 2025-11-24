package com.bim.seif.models.dto;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

public class PactarTipoCambioRequest {

    @NotNull
    @Min(value = 1, message = "El tipo de cambio debe ser >= 1")
    private Integer tipoCambio; // p.ej. 20 (significa 1 USD = 20 MXP)

    @NotBlank
    private String contacto;

    @NotBlank
    private String claveLlamada;

    // getters/setters
    public Integer getTipoCambio() { return tipoCambio; }
    public void setTipoCambio(Integer tipoCambio) { this.tipoCambio = tipoCambio; }
    public String getContacto() { return contacto; }
    public void setContacto(String contacto) { this.contacto = contacto; }
    public String getClaveLlamada() { return claveLlamada; }
    public void setClaveLlamada(String claveLlamada) { this.claveLlamada = claveLlamada; }
}
