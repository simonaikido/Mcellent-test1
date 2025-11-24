package com.bim.seif.models.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VacacionesPrelogResponse {
    private Long asignacionId;
    private int monetariasPrelog;
    private int programadasPrelog;
    private int juridicasPrelog;
    private int updatedRows;
}