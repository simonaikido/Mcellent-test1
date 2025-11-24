	
package com.bim.seif.models;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Data
@Entity
public class Region {

    @Id
    private String cve;
    @Column(nullable = false)
    private String descripcion;
}