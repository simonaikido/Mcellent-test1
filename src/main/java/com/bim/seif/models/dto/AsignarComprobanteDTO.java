package com.bim.seif.models.dto;

import java.util.List;

import lombok.Data;

@Data
public class AsignarComprobanteDTO {
    private Long comprobanteId;
    private String tipoOperacion;     // "MONETARIA" o "JURIDICA"
    private List<Long> operacionIds;  // [1,2,3...]
}
