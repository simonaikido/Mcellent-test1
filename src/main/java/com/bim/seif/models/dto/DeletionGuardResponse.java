package com.bim.seif.models.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeletionGuardResponse {
    private boolean deletable;
    private long monetarias; // pendientes o en proceso
    private long juridicas; // pendientes o en proceso
    private long programadas; // pendientes o en proceso

    @Singular("reason") // <- crea builder.reason("...") y builder.reasons(List)
    private List<String> reasons;
}
