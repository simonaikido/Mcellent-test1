package com.bim.seif.models;

import lombok.Data;

import javax.persistence.*;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

@Data
@Entity
@Table(name = "solicitud_documento_operacion_juridica")
public class SolicitudArchivoJuridica  {

    /*private Long id;
    private Date fecha_solicitud;
    private Date fecha_carga;
    private String nombre_archivo;
    private String descripcion_del_acto;
    private String nombre_formato;
    private String nota;*/

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // IDENTITY para autoincrement (SQL Server)
    private Long id;  // Cambiado a Integer porque en SQL es INT

    @Column(name = "id_operacion", nullable = false)
    private Long idOperacion;  // bigint en SQL → Long en Java

    @Column(name = "fecha_solicitud", nullable = false)
    @Temporal(TemporalType.DATE) // Solo fecha, sin hora
    private Date fechaSolicitud;

    @Column(name = "fecha_carga")
    @Temporal(TemporalType.DATE) // Solo fecha, sin hora
    private Date fechaCarga;

    @Column(name = "nombre_archivo", length = 255)
    private String nombreArchivo;

    @Column(name = "ruta_archivo", length = 255)
    private String rutaArchivo;

    @Column(name = "descripcion_del_acto", length = 255)
    private String descripcionDelActo;

    @Column(name = "nombre_formato", length = 255)
    private String nombreFormato;

    @Column(name = "ruta_formato", length = 255)
    private String rutaFormato;

    @Column(name = "nota", length = 255)
    private String nota;

    // Relación ManyToOne con OperacionJuridica (si existe esa entidad)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_operacion", insertable = false, updatable = false) // Evita duplicar la columna
    private OperacionJuridica operacionJuridica;

    

   
}
