package com.bim.seif.models.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class ComprobanteOperacionDto implements Serializable {
    private Long id;
    private String rutaComprobante;
    private LocalDateTime fechaCarga;
}
