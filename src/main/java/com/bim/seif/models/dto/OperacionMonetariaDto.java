package com.bim.seif.models.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
public class OperacionMonetariaDto implements Serializable {
    private final Long id;
    private final CampoCuentaCargoDto cuentaCargo;
    private final CampoCuentaAbonoDto cuentaAbono;
    private final String concepto;
    private final String comentario;
    private final LocalDateTime fechaRegistro;
    private final InstruccionMonetariaDto instruccion;
    private final TipoOperacionMonetariaDto tipoOperacion;
    private final ComprobanteOperacionDto comprobante;
    private List<RevisionOperacionMonetariaDto> aprobaciones;
    private Set<SolicitudCorreccionDto> solicitudesCorreccion;
    private final CampoMontoDto monto;
    private final CampoReferenciaDto referencia;
}
