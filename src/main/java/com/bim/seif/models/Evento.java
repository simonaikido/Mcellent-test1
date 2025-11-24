package com.bim.seif.models;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
public class Evento {

    @Id
    @Enumerated(EnumType.STRING)
    private TipoEvento cve;
    private String nombre;
    private String asunto;
    @Column
    private String cuerpoCorreo;
    private String rutaFirma;
    private String modificadoPor;
    @Column(length = 1000)
    private String comentario;
    private boolean desactivado;
    private LocalDateTime fechaModificacion;
    @OneToMany(mappedBy = "evento")
    List<EventoAuditoria> modificaciones;

    @PreUpdate
    protected void onUpdate() {
        fechaModificacion = LocalDateTime.now();
    }

}
