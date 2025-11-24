package com.bim.seif.models;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
public class SolicitudCorreccionOperacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(length = 1000)
    private String comentarios;
    private LocalDateTime fechaRevision;
    private LocalDateTime fechaCorreccion;

    @ManyToOne
    @JoinColumn(name = "operacion_monetaria_id")
    private OperacionMonetaria operacionMonetaria;

    //
//    @ManyToOne
    @Column(nullable = false)
    private String validadorEmail;

    @PrePersist
    protected void onCreate() {
        this.fechaRevision = LocalDateTime.now();
    }
}
