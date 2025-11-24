package com.bim.seif.repositories;

import com.bim.seif.models.Instruccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface InstruccionRepository extends JpaRepository<Instruccion, String> {

    // @Query("SELECT i FROM Instruccion i LEFT JOIN InstruccionMonetaria im ON
    // i.folio = im.folio WHERE im.folio IS NULL")
    @Query("""
            SELECT i
            FROM Instruccion i
            WHERE i.fechaRechazo IS NULL
              AND i.fideicomiso.region.cve IN :regiones
              AND NOT EXISTS (SELECT 1 FROM InstruccionMonetaria im WHERE im.folio = i.folio)
              AND NOT EXISTS (SELECT 1 FROM InstruccionJuridica ij  WHERE ij.folio = i.folio)
              AND NOT EXISTS (SELECT 1 FROM InstruccionProgramada ip WHERE ip.folio = i.folio)
            """)
    List<Instruccion> findInstruccionesWithoutSubtipe(@Param("regiones") Set<String> regiones);

    Optional<Instruccion> findInstruccionesByFolio(String folio);
}
