package com.bim.seif.models;


import javax.persistence.Entity;
import javax.persistence.Id;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@NoArgsConstructor
public class EstatusInstruccion {

    public EstatusInstruccion(String cve){
        this.cve = cve;
    }

    @Id
    private String cve;
    private String Descripcion;
}
