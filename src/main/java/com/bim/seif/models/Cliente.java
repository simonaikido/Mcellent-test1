package com.bim.seif.models;

import javax.persistence.*;

import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true)
    private String email;
    private String nombre;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private boolean propietario;
    private String idioma;
    private String password;
    private LocalDateTime fechaAlta;
    private LocalDateTime fechaBaja;

    @PrePersist
    protected void onCreate() {
        fechaAlta = LocalDateTime.now();
    }

}
