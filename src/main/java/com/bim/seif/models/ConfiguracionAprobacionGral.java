package com.bim.seif.models;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
public class ConfiguracionAprobacionGral {

    @Id
    private String cve; // NIVEL_1, NIVEL_2
    // unico y es el orden por el que debe pasar la validacion
    @Column()
    private int orden;
    //Gerente tecnico legal y administrativo // solo esos roles
    @Enumerated(EnumType.STRING)
    private Rol rol;


}

