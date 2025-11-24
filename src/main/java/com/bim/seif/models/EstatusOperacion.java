package com.bim.seif.models;

import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.Id;

@Data
@Entity
@NoArgsConstructor
public class EstatusOperacion {
    public EstatusOperacion(String cve){
        this.cve = cve;
    }

    @Id
    private String cve;
    private String Descripcion;
}
