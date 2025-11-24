package com.bim.seif.repositories;

import com.bim.seif.models.OperacionMonetaria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OperacionMonetariaRepository extends JpaRepository<OperacionMonetaria, Long> {
   List<OperacionMonetaria> findByInstruccionFolio(String folioInstruccion);

   @Query("SELECT om FROM OperacionMonetaria om LEFT JOIN om.aprobaciones ra WHERE ra IS NULL OR ra.omitida = FALSE")
   List<OperacionMonetaria> findOperacionesMonetariasSinAprobacionesOmitidas();

   @Query("SELECT om FROM OperacionMonetaria om LEFT JOIN om.aprobaciones ra WHERE om.instruccion.folio = :folioInstruccion AND (ra IS NULL OR ra.omitida = FALSE)")
   List<OperacionMonetaria> findByInstruccionFolioAndAprobacionesNotOmitidas(String folioInstruccion);

   // ---------- Métodos para comprobantes (nuevos) ----------

   // Para validación: ¿cuántas operaciones de la instrucción aún no tienen
   // comprobante?
   long countByInstruccion_FolioAndComprobanteIsNull(String folioInstruccion);

   // IDs de operaciones por folio (útil para marcar enviados)
   @Query("""
             SELECT om.id
             FROM OperacionMonetaria om
             WHERE om.instruccion.folio = :folio
         """)
   List<Long> findIdsByInstruccionFolio(@Param("folio") String folio);

   // IDs de comprobante (solo los no nulos) para un set de operaciones
   @Query("""
             SELECT om.comprobante.idComprobante
             FROM OperacionMonetaria om
             WHERE om.id IN :ids
               AND om.comprobante IS NOT NULL
         """)
   List<Long> findComprobanteIdsByOperacionIds(@Param("ids") List<Long> ids);

   List<OperacionMonetaria> findAllByInstruccion_Folio(String folioInstruccion);
}