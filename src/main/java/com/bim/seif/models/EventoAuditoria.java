package com.bim.seif.models;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity(name ="modificacion_evento")
public class EventoAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String asunto;
    private String comentario;
    private String cuerpoCorreo;
    private String rutaFirma;
    private String modificadoPor;
    private boolean desactivado;
    private LocalDateTime fechaModificacion;
    @ManyToOne
    private Evento evento;

}
