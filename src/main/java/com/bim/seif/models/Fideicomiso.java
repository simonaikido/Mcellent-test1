package com.bim.seif.models;

import javax.persistence.*;
import lombok.Data;

import java.util.List;

@Data
@Entity
public class Fideicomiso {

    @Id
    private String folio;
    @ManyToOne
    private Region region;
    private String alias;
    private String nombreCliente;
    private Boolean instruccionesMonetarias;
    private Boolean instruccionesJuridicas;
    private String encargadoEmail;

    @OneToMany(mappedBy = "fideicomiso")
    private List<Contrato> contratos;


}
