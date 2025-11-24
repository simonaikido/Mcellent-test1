package com.bim.seif.models;


import javax.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "INSTRUCCION")
//@Inheritance(strategy = InheritanceType.JOINED)
public class Instruccion {

    @Id
    private String folio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fideicomiso_folio", nullable = false)
    private Fideicomiso fideicomiso;

//    @OneToOne(mappedBy = "instruccion")
//    private InstruccionMonetaria instruccionMonetaria;

    @Column(nullable = false)
    private String rutaArchivo;
    @Column(length = 1000)
    private String comentario;
    @Column(nullable = false)
    private String clienteCarga;
    @Column(nullable = false)
    private LocalDateTime fechaAlta;
    private LocalDateTime fechaAtencion;
    @Enumerated(EnumType.STRING) // O EnumType.ORDINAL
    private TipoInstruccion tipo;
    //fix quitar de aqui y poner encada tipo de instruccion
    private LocalDateTime fechaCancelacion;
    private LocalDateTime fechaRechazo;
    private LocalDateTime fechaAprobacion;
        @PrePersist
    protected void onCreate() {
        fechaAlta = LocalDateTime.now();
    }

}