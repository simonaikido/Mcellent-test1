package com.bim.seif.models;

//import jakarta.persistence.Entity;
//import jakarta.persistence.Id;
//import jakarta.persistence.JoinColumn;
//import jakarta.persistence.ManyToOne;
import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
public class RevisionOperacionJuridica {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private boolean aprobada;
    private boolean omitida;
    @Column(length = 1000)
    private String comentarios;
    private LocalDateTime fechaRevision;


    @ManyToOne
    @JoinColumn(name = "operacion_juridica_id")
    private OperacionJuridica operacionJuridica;

//    @ManyToOne empleadoEmail 
    private String validadorEmail;
}
