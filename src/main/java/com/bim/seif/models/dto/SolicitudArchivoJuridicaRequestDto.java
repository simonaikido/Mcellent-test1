package com.bim.seif.models.dto;

import java.util.List;

import javax.validation.constraints.NotNull;

import lombok.Data;

@Data
public class SolicitudArchivoJuridicaRequestDto {
	@NotNull
	private Long id;

	private String descripcionDelActo;
	private String nota;

	private List<String> nombreArchivo;
	private List<String> nombreFormato;
}
