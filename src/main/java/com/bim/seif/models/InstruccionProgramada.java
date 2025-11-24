package com.bim.seif.models;

import java.time.LocalDateTime;

import javax.persistence.*;

import lombok.Data;

@Data
@Entity
@Table(name = "instruccion_programada")
public class InstruccionProgramada {

    @Id
    private String folio;

    @OneToOne
    @MapsId // La clave primaria proviene de Instruccion
    @JoinColumn(name = "folio")
    private Instruccion instruccion;
    private String responsable_email;
    @ManyToOne
    private TipoOperacionMonetaria tipoOperacionMonetaria;
    private String concepto;
    private String comentario;
    @ManyToOne
    @JoinColumn(name = "cuenta_abono_cuenta")
    private CuentaAbono cuenta;
    @ManyToOne
    @JoinColumn(name = "cuenta_cargo", referencedColumnName = "id")
    private CampoCuentaCargo cuentaCargo;
    private int monto;
    private String referencia;
    @ManyToOne
    private Divisa divisa;
    private LocalDateTime fechaCancelacion;
    private LocalDateTime fechaClasificacion;

    protected void onCreate() {
        fechaClasificacion = LocalDateTime.now();
    }

}