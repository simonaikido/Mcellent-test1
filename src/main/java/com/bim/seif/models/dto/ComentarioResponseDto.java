// src/main/java/com/bim/seif/dtos/ComentarioResponseDTO.java
package com.bim.seif.models.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ComentarioResponseDto {
    private Long id;
    private String comentario;
    private boolean leeida;
    private LocalDateTime fechaComentario;
    private String tipoComentario;
    private String empleadoEmail;
    private String folioInstruccion; // Solo el folio de la instrucción
    // Podrías añadir más campos si los necesitas del Instruccion, pero solo los necesarios y serializables
}