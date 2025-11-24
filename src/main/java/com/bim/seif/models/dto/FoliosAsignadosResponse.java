package com.bim.seif.models.dto;

import lombok.*;
import java.util.List;

@Getter @Setter @Builder
@AllArgsConstructor @NoArgsConstructor
public class FoliosAsignadosResponse {
    private String email;

    private List<String> monetarias;   // folios monetarios NO programados
    private List<String> programadas;  // folios monetarios programados
    private List<String> juridicas;    // folios jurídicos

    public int getMonetariasCount() { return monetarias != null ? monetarias.size() : 0; }
    public int getProgramadasCount() { return programadas != null ? programadas.size() : 0; }
    public int getJuridicasCount()   { return juridicas  != null ? juridicas.size()  : 0; }
}