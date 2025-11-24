package com.bim.seif.models.dto;

import lombok.*;
import java.time.OffsetDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmpleadoAuditDto {
    private String uid;
    private String accion;
    private String actor;
    private String ip;
    private String userAgent;
    private String payloadBefore;
    private String payloadAfter;
    private OffsetDateTime createdAt;
    private String comentario;
}
