
package com.bim.seif.models;

import javax.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Entity
public class OperacionMonetaria {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String concepto;
    private String comentario;
    private String observacion;
    private LocalDateTime fechaRegistro;
    private LocalDateTime fechaRevision;
    private Boolean aprobada;
    private Boolean omitida;

    @ManyToOne
    @JoinColumn(name = "instruccion_folio", nullable = false)
    private InstruccionMonetaria instruccion;

    @ManyToOne
    @JoinColumn(name = "tipo_operacion_cve", nullable = false)
    private TipoOperacionMonetaria tipoOperacion;

    @ManyToOne
    @JoinColumn(name = "id_comprobante", nullable = true)
    private ComprobanteOperacion comprobante;

    @OneToMany(mappedBy = "operacionMonetaria", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<RevisionOperacionMonetaria> aprobaciones;

    @OneToMany(mappedBy = "operacionMonetaria", cascade = CascadeType.ALL, fetch = FetchType.LAZY) // Puedes dejar LAZY
                                                                                                   // aquí
    private List<Campo> campos;

    @PrePersist
    protected void onCreate() {
        // Solo poner ahora si NO viene desde el flujo (programadas)
        if (this.fechaRegistro == null) {
            this.fechaRegistro = LocalDateTime.now();
        }
    }

}
