package com.bim.seif.models;

import lombok.Data;

import javax.persistence.*;
import java.sql.Timestamp;

@Data
@Entity
@Table(name = "programacion")
public class Programacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "fecha_ejecucion")
    private Timestamp fechaEjecucion;

    /*@ManyToOne
    @JoinColumn(name = "folio")*/
    @Column(name = "instruccion_progamada_folio")
    private String instruccionProgramada;

}
