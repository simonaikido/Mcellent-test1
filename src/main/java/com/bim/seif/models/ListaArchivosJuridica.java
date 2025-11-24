package com.bim.seif.models;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Data
@Entity
@Table(name = "lista_archivos_juridica")
public class ListaArchivosJuridica {

    @Id
    private Long id;
    private String nombre_archivo;
    private String descripcion;
}

/*
CREATE TABLE [dbo].[lista_archivos_juridica](
    [id] INT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    [nombre_archivo] VARCHAR(255) NOT NULL,
    [descripcion] VARCHAR(255) NULL,
 */