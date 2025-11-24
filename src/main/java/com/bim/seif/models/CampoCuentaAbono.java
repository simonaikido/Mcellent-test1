package com.bim.seif.models;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.PrimaryKeyJoinColumn;

import lombok.Data;

@Data
@Entity
@PrimaryKeyJoinColumn(name = "id")
public class CampoCuentaAbono extends Campo{
    @ManyToOne
    @JoinColumn(name = "cuenta_abono_cuenta")
    private CuentaAbono cuenta;
}