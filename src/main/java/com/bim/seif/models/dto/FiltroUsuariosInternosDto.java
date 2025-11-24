package com.bim.seif.models.dto;

import lombok.Data;

@Data
public class FiltroUsuariosInternosDto {

  private String celular;        // mobile
  private String extension;      // telephoneNumber
  private String estatus;        // ACTIVE / INACTIVE (employeeType)
  private String usuarioAlta;    // si lo manejas en LDAP (atributo propio)
  private String usuarioBaja;    // si lo manejas en LDAP (atributo propio)
  private String auditoriaBaja;  // si lo manejas en LDAP (atributo propio)

  // Qué campo de fecha filtrar con (desde/hasta)
  private CampoFecha campoFecha = CampoFecha.FECHA_ALTA;

  public enum CampoFecha {
    FECHA_ALTA,        // createTimestamp
    FECHA_BAJA,        // bajaTimestamp (ajústalo si tu atributo se llama diferente)
    FECHA_ULTIMO_LOGUEO // authTimestamp (ajústalo si usas otro)
  }
}