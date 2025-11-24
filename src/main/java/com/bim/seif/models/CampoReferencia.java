package com.bim.seif.models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.PrimaryKeyJoinColumn;
import javax.persistence.Table;

import lombok.Data;

@Data
@Entity
@PrimaryKeyJoinColumn(name = "id")
public class CampoReferencia extends Campo{
    @Column(name = "referencia")
    private String referencia;
}
