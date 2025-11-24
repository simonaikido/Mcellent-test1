package com.bim.seif.models;


import javax.persistence.*;

import lombok.Data;

@Data
@Entity
@PrimaryKeyJoinColumn(name = "id")
public class CampoCuentaCargo extends Campo {

    private String cuenta;
    private String banco;
    @ManyToOne
    private Divisa divisa;

}
