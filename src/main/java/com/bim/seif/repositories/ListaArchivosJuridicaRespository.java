package com.bim.seif.repositories;

import com.bim.seif.models.ListaArchivosJuridica;
import feign.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ListaArchivosJuridicaRespository extends JpaRepository<ListaArchivosJuridica, Long> {


    @Query(value = """
               SELECT
                   STRING_AGG(T2.nombre_archivo, ',') AS nombres_archivos
               FROM (
                   SELECT
                       value AS id_archivo
                   FROM (
                       SELECT
            [archivos_solicitados]
                       FROM [dbo].[configuracion_solicitudarchivo_tipooperacion]
                       WHERE tipo_operacion = :cve
                   ) AS T1
                   CROSS APPLY STRING_SPLIT(T1.archivos_solicitados, ',')
               ) AS T_SPLIT
               INNER JOIN [dbo].[lista_archivos_juridica] AS T2
                   ON T_SPLIT.id_archivo = CAST(T2.id AS NVARCHAR(MAX));
            """, nativeQuery = true)
    String findArchivosSujeridosPorTipoOperacionJuridica(@Param("cve") String cve);

}
