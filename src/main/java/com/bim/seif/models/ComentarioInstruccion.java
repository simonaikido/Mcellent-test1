package com.bim.seif.models;

import javax.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
public class ComentarioInstruccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String comentario;
    private boolean leeida;
    private LocalDateTime fechaComentario;
    private String tipoComentario; //Rechazo, interno, al cliente 

    @ManyToOne
    @JoinColumn(name = "instruccion_folio")
    private Instruccion instruccion;
//    @ManyToOne
//    @JoinColumn(name = "empleado_email")
    private String empleadoEmail;
}
