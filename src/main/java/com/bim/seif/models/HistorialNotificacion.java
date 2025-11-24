package com.bim.seif.models;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
public class HistorialNotificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String nombre;
    private String titulo;
    @Column(length = 1000)
    private String mensaje;
    private String modificadoPor;
    @Column(length = 1000)
    private String comentario;
    private Boolean desactivada;
    private LocalDateTime fechaModificacion;
    @ManyToOne
    @JoinColumn(name = "notificacion_cve")
    private Notificacion notificacion;
}
