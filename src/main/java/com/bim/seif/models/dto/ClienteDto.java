package com.bim.seif.models.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ClienteDto {

    private Long id;
    private String email;
    private String telefono;
    private String nombre;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private String facultad;
    private String idioma;
    private LocalDateTime fechaAlta;
    private LocalDateTime fechaBaja;
    private LocalDateTime fechaInicioSesion;
    private boolean propietario;


}
