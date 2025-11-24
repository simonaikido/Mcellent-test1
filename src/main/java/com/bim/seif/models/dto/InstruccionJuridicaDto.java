package com.bim.seif.models.dto;

import com.bim.seif.models.EstatusInstruccion;
import lombok.Data;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.List;

@Data
public class InstruccionJuridicaDto implements Serializable {
    private String folio;
    private InstruccionDto instruccion;
    private boolean  urgente;
    private boolean operadaParcialmente;
    private Timestamp fechaModificacion;
    private String nombreClienteInstructor;
    private Timestamp fechaEnvioAprobacion;
    private EstatusInstruccion estatus;

    private String aliasFideicomiso;
    private String rutaArchivo;
    private String tipoInstruccion;
    private Timestamp fechaHoraAlta;
    private String fechaHoraFin;
    private String folioFideicomiso;
    private String regionFideicomiso;
    private String nombreFideicomiso;
    private String estatusCve;
    private String responsable;
    private List<OperacionJuridicaDto> operaciones;
    private String comentario;
    private boolean solicitudCorreccion;
    private String fechaAprobacion;
    private String clienteInstruccion;
    private String correoClienteInstruccion;

    private String regionCve;
    private String regionDesc;



}
