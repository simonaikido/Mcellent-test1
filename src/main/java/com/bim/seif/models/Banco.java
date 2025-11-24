package com.bim.seif.models;

import javax.persistence.Entity;
import javax.persistence.Id;
import lombok.Data;

@Data
@Entity
public class Banco {

    @Id
    private String cve;
    private String descripcion;
}
