package com.bim.seif.models.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class FideicomisoContactoDto implements Serializable {
    // datos fideicomiso (inmutables como ya los tienes)
    private final String folio;
    private final String alias;
    private final String tipo;
    private final String subtipo;

    // datos del cliente (inmutables que ya tienes)
    private final boolean bloqueado;
    private final String nombre;
    private final String telefono;
    private final String email;
    private final String facultad;

    // existentes (mutables que ya tienes)
    private Boolean activo;
    private LocalDateTime fechaBaja;
    private String idioma;

    //(mutables, opcionales) PARA COMPARAR BD vs MICRO

    /** Facultad/participación guardada en BD (Contrato.participacion) */
    private String facultadDb;

    /** Facultad/participación que llega del micro (refleja el valor de "facultad") */
    private String facultadMicro;

    /** Idioma existente en BD para este cliente (Cliente.idioma) */
    private String idiomaDb;

    /** Idioma reportado por el micro (si más adelante lo envían; por ahora puedes igualarlo a BD) */
    private String idiomaMicro;

    /** true si hay diferencias relevantes entre BD y micro */
    private Boolean desincronizado;

    /** Lista de mensajes con diferencias detectadas (legibles para UI) */
    private List<String> cambios;
}