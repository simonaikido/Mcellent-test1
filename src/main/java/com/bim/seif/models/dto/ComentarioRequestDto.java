// src/main/java/com/bim/seif/dtos/ComentarioRequestDTO.java
package com.bim.seif.models.dto;

import lombok.Data;

@Data
public class ComentarioRequestDto {
    private String comentario;
    private String empleadoEmail;
    private String tipoComentario;
}