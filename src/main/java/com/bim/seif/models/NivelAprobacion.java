package com.bim.seif.models;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.Id;
import lombok.Data;

@Data
@Entity
public class NivelAprobacion {

    @Id
    private String cve; // NIVEL_1, NIVEL_2
    // monto desde el que se requiere esta aprobacion en pesos mexicanos
    private Long monto;
    @Enumerated(EnumType.STRING)
    private Rol rol;

}
