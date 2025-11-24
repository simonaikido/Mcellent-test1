package com.bim.seif.models;

import javax.persistence.*;

import lombok.Data;

@Data
@Entity
public class ConfiguracionNivelAprobacionMonto {

    @Id
    private String cve; // NIVEL_1, NIVEL_2
    // monto desde el que se requiere esta aprobacion en pesos mexicanos
    @Column(nullable = false)
    private Long monto;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Rol rol;

}
