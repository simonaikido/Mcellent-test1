package com.bim.seif.models;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
public class Notificacion {

    @Id
    @Enumerated(EnumType.STRING)
    private TipoNotificacion cve;
    private String nombre;
    private String titulo;
    @Column(length = 1000)
    private String mensaje;
    private String modificadoPor;
    @Column(length = 1000)
    private String comentario;
    private LocalDateTime fechaModificacion;
    private boolean desactivada;
    @OneToMany(mappedBy = "notificacion")
    private List<HistorialNotificacion> modificaciones;

    @PreUpdate
    protected void onUpdate(){
        fechaModificacion = LocalDateTime.now();
    }
}
