package com.bim.seif.models.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class RevisionOperacionJuridicaDto implements Serializable{
    private long id;
    private boolean aprobada;
    private boolean omitida;
    private String comentarios;
    private LocalDateTime fechaRevision;
    private String validadorEmail;
}
