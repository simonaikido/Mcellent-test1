package com.bim.seif.models.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class OperacionMonetariaRequestDto implements Serializable {
    private Long id;
    private TipoOperacionMonetariaDto tipoOperacion;
    private String instruccionFolio;
    private String concepto;
    private String comentario;
    private String observacion;
    private Boolean aprobada;
    private LocalDateTime fechaRegistro;

    private CampoMontoDto monto;
    private CampoReferenciaDto referencia;
    private CampoCuentaCargoDto cuentaCargo;
    private CampoCuentaDto cuentaAbono;

}
