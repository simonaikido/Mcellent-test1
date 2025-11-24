package com.bim.seif.models;

import javax.persistence.*;
import lombok.Data;

    @Data
    @Entity
    @Table(name = "TIPO_CAMPO")
    public class TipoCampo {

        @Id
        private String cve;
        @Column(nullable = false)
        private String descripcion;

    }
