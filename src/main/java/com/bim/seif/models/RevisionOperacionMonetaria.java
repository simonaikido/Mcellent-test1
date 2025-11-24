package com.bim.seif.models;

import javax.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
public class RevisionOperacionMonetaria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false)
    private boolean aprobada;
    @Column(nullable = false)
    private boolean omitida;
//    @Column(nullable = false)
//    private boolean completa;

    @Column(nullable = false, length = 1000)
    private String comentarios;
    private LocalDateTime fechaRevision;
 
    @ManyToOne
    @JoinColumn(name = "operacion_monetaria_id")
    private OperacionMonetaria operacionMonetaria;

//
//    @ManyToOne
    @Column(nullable = false)
    private String validadorEmail;

    @Enumerated(EnumType.STRING)
    private Rol rol;
    
    @PrePersist
    protected void onCreate() {
        this.fechaRevision = LocalDateTime.now();
    }


}