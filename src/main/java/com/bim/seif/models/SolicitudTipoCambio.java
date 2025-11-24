package com.bim.seif.models;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
public class SolicitudTipoCambio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "contacto")
    private String contacto;
    @Column(name = "clave_llamada")
    private String claveLlamada;
    @Column(name = "tipo_cambio")
    private int tipoCambio;
    @ManyToOne
    private Divisa divisaPago;
    @ManyToOne
    private Divisa divisaCompra;
    private LocalDateTime fechaSolicitud;
    private LocalDateTime fechaFinalizacion;
    private LocalDateTime fechaRechazo;
    
    @ManyToOne
    @JoinColumn(name = "operacion_id")
    private OperacionMonetaria operacion;

}
