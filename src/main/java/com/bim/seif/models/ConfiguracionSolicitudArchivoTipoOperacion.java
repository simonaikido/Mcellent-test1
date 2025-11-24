package com.bim.seif.models;

import lombok.Data;

import javax.persistence.*;

@Data
@Entity
@Table(name = "configuracion_solicitudarchivo_tipooperacion")
public class ConfiguracionSolicitudArchivoTipoOperacion {
    @Id
    private Long id;
    @ManyToOne
    @JoinColumn(name = "tipo_operacion_juridica_cve", referencedColumnName = "cve")
    private TipoOperacionJuridica tipoOperacionJuridica;
    private String archivos_solicitados;
}

/*

[id]
      ,[tipo_operacion]
      ,[archivos_solicitados]
  FROM [dbo].[configuracion_solicitudarchivo_tipooperacion]
 */