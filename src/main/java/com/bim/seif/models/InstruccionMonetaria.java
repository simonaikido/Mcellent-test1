package com.bim.seif.models;

import javax.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
@Table(name = "INSTRUCCION_MONETARIA")
public class InstruccionMonetaria{



    @Id
    private String folio;

    @OneToOne
    @MapsId // La clave primaria proviene de Instruccion
    @JoinColumn(name = "folio")
    private Instruccion instruccion;

//    @ManyToOne
//    @JoinColumn(name = "responsable_email")
    private String responsable;
    
    private String validadorEmail;

    private boolean  prioritaria;
    private boolean operadaParcialmente;
    // observaciones de mesa de control cuando la instruccion esta ya finalizada
    @Column(length = 1000)
    private String observaciones;

    @ManyToOne
    @JoinColumn(name = "estatus_cve")
    private EstatusInstruccion estatus;
    private LocalDateTime fechaModificacion;
    private LocalDateTime fechaClasificacion;
    private LocalDateTime fechaCancelacion;
    private LocalDateTime fechaAprobacion;
    private boolean solicitudCorreccion;
    private boolean programada;
    private boolean mesaControl;

    @OneToMany(mappedBy = "instruccion", fetch = FetchType.EAGER)
    private List<OperacionMonetaria> operaciones;

    @PrePersist
    protected void onCreate() {
        fechaClasificacion = LocalDateTime.now();
    }

}