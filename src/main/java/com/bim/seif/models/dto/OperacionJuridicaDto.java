package com.bim.seif.models.dto;

import com.bim.seif.models.TipoOperacionJuridica;
import lombok.Data;
import net.minidev.json.annotate.JsonIgnore;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OperacionJuridicaDto implements Serializable {
    private Long id;
    private LocalDateTime fechaRegistro;
    @JsonIgnore
    private InstruccionJuridicaDto instruccion;
    private TipoOperacionJuridica tipoOperacionJuridica;
    private String descripcionOperacion;
    private String comentario;
    private String observaciones;
    private String clienteCorreoElectronico;
    private boolean instruccionCumpleFines;
    private boolean boolClienteEmail;
    private List<RevisionOperacionJuridicaDto> revisiones;
    private boolean firmasCorrectas;
    private String estatusCve;
    private List<SolicitudArchivoJuridicaDto> solicitudArchivoJuridica;

    private boolean envioNotaria;

    private LocalDate fechaEcritura;
    private Long numeroEscritura;
    private LocalDate fechaMantenimiento;
    private Long secuenciaCartaCOmplemento;
}
