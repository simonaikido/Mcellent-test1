package com.bim.seif.models.wrappers;

import java.util.List;

import javax.validation.constraints.NotEmpty;

import com.bim.seif.models.dto.OperacionJuridicaRequestDto;
import com.bim.seif.models.dto.SolicitudArchivoJuridicaRequestDto;

import lombok.Data;

@Data
public class OperacionesJuridicasWrapper {
    @NotEmpty
    private List<OperacionJuridicaRequestDto> operaciones;
    private List<SolicitudArchivoJuridicaRequestDto> solicitudDeArchivos;
}