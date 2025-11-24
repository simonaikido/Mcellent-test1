package com.bim.seif.models.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SolicitudTipoCambioDto {
    private Long id;
    private String folio;
    private String cuentaCargo;
    private String cuentaAbono;
    private String monto;
    private String referencia;
    private String comentarios;
    private String concepto;
    private String tipoCambio;
    private String divisa;
    private String banco;
    private String divisaCompra;
    private String divisaPago;
    private String contacto;
    private String claveLlamada;
    private String divisaPagoDescripcion;
    private String divisaCompraDescripcion;
    private Integer tipoCambioPagoNacional;
    private Integer tipoCambioCompraNacional;
}