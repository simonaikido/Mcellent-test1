package com.bim.seif.models.dto;

import com.bim.seif.models.Rol;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class RevisionOperacionMonetariaDto implements Serializable {
    private long id;
    private boolean aprobada;
    private boolean omitida;
    private Rol rol;
    private String comentarios;
    private LocalDateTime fechaRevision;
    private String validadorEmail;
}





