package com.bim.seif.models;

import javax.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@PrimaryKeyJoinColumn(name = "id")
public class CuentaAbono {
    @Id
    private Long cuenta;
    private String banco;

    @ManyToOne
    private Divisa divisa;
    private String beneficiario;
    private String direccion;
    private String rfc;
    private LocalDateTime fechaBaja;
    private String regimen;
    private String rutaArchivo;
    private LocalDate fechaAlta;

    @ManyToOne
    @JoinColumn(name = "fideicomiso_folio")
    private Fideicomiso fideicomiso;

    @Column(name = "solicitud_cuenta")
    private Boolean solicitudCuenta;
}
