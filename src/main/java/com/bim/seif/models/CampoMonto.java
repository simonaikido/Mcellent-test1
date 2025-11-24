	
package com.bim.seif.models;

import javax.persistence.*;

import lombok.Data;

@Data
@Entity
@PrimaryKeyJoinColumn(name = "id")
public class CampoMonto extends Campo{
    @Column(name = "monto", nullable = false)
    private Long monto;

    @ManyToOne
    @JoinColumn(name = "divisa_cve", nullable = false)
    private Divisa divisa;

    @Column(name = "tipo_cambio", nullable = false)
    private int tipoCambio;
}