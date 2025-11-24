package com.bim.seif.repositories;

import com.bim.seif.models.OperacionJuridica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OperacionJuridicaRepository extends JpaRepository<OperacionJuridica, Long> {
    List<OperacionJuridica> findByInstruccionFolio(String folioInstruccion);

    // Metodo para encontrar operaciones por el tipo de operación y su estatus
        List<OperacionJuridica> findByTipoOperacionJuridica_CveInAndEstatus_Cve(List<String> tipoOperacionCves,
                        String estatusCve);

        // ---------- Métodos para comprobantes (nuevos) ----------

        long countByInstruccion_FolioAndComprobanteIsNull(String folioInstruccion);

        @Query("""
                            SELECT oj.id
                            FROM OperacionJuridica oj
                            WHERE oj.instruccion.folio = :folio
                        """)
        List<Long> findIdsByInstruccionFolio(@Param("folio") String folio);

        @Query("""
                            SELECT oj.comprobante.idComprobante
                            FROM OperacionJuridica oj
                            WHERE oj.id IN :ids
                              AND oj.comprobante IS NOT NULL
                        """)
        List<Long> findComprobanteIdsByOperacionIds(@Param("ids") List<Long> ids);

        
}