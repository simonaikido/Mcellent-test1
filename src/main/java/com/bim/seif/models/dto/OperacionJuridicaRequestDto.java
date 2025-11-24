package com.bim.seif.models.dto;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import lombok.Data;

@Data
public class OperacionJuridicaRequestDto {

	@NotNull
    private Long id;

    @NotBlank
    private String descripcionOperacion;

    private String comentario;
    private String observaciones;

    private Boolean instruccionCumpleFines;
    private Boolean firmasCorrectas;
    private Boolean boolClienteEmail;

    @Valid
    @NotNull
    private TipoOperacionJuridicaDto tipoOperacionJuridica;
}
