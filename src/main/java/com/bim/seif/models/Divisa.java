package com.bim.seif.models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import lombok.Data;

@Data
@Entity
public class Divisa {

    @Id
    private String cve;
    private String descripcion;
    @Column(nullable = false)
    private boolean nacional;
    @Column(name = "tipo_cambio_moneda_nacional" ,nullable = false)
    private int tipoCambioMonedaNacional;
}