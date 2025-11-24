package com.bim.seif.models.dto;

import com.bim.seif.models.InstruccionProgramada;

import java.io.Serializable;
import java.sql.Timestamp;

public class ProgramacionDto implements Serializable {
    private Timestamp fechaEjecucion;
    private InstruccionProgramada instruccionProgramada;
}
