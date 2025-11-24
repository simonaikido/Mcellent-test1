package com.bim.seif.models;

import javax.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Entity
public class Contrato {

    @EmbeddedId
    private ContratoId id;

    @ManyToOne
    @MapsId("fideicomisoFolio")
    @JoinColumn(name = "fideicomiso_folio")
    private Fideicomiso fideicomiso;

    @ManyToOne
    @MapsId("clienteId")
    @JoinColumn(name="cliente_id")
    private Cliente cliente;

//    @Enumerated(EnumType.STRING)
//    private Participacion participacion;

    private String participacion;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @EqualsAndHashCode
    @Embeddable
    public static class ContratoId implements Serializable {
        private static final long serialVersionUID = 796659461500775491L;
        private String fideicomisoFolio;
        private Long clienteId;
    }
}
