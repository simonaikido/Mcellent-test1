package com.bim.seif.models;

import java.time.LocalDateTime;
import javax.persistence.*;
import lombok.Data;


@Entity
@Data
public class ComprobanteOperacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_comprobante")
    private Long idComprobante;

    @Column(name = "fecha_carga")
    private LocalDateTime fechaCarga;

    @Column(name = "ruta_comprobante")
    private String rutaComprobante;

    @Column(name = "status")
    private Boolean status; // true=enviado; false/null=solo guardado

        public ComprobanteOperacion() {
        //TODO Auto-generated constructor stub
    }
}
