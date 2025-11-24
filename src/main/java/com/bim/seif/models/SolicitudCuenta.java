package com.bim.seif.models;

import javax.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
public class SolicitudCuenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String contacto;
    private Long cuenta;
    private String banco;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "divisa_cve", referencedColumnName = "cve")
    private Divisa divisa_cve;
    private String beneficiario;
    private String rfc;
    private String direccion;
    private String rutaEdoCta;

    private LocalDateTime fechaSolicitud;
    private LocalDateTime fechaCarga;
    private LocalDateTime fechaFinalizacion;

    //El alta de cuenta esta condicionada a que las operaciones de la instruccion esten aprobadas
    @ManyToOne
    @JoinColumn(name = "instruccion_folio")
    private Instruccion instruccion;

    @ManyToOne
    @JoinColumn(name = "fideicomiso_folio")
    private Fideicomiso fideicomiso_folio;

}
