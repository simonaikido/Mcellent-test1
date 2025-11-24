package com.bim.seif.repositories;

import com.bim.seif.models.SolicitudCuenta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SolicitudCuentaRepository extends JpaRepository<SolicitudCuenta, Long> {

    // Método usando @Query para encontrar solicitudes por el folio del fideicomiso
    // La consulta JPQL referencia directamente el campo 'fideicomiso_folio' de
    // SolicitudCuenta
    // y luego el campo 'folio' de la entidad Fideicomiso relacionada.
    @Query("SELECT sc FROM SolicitudCuenta sc WHERE sc.fideicomiso_folio.folio = :folioFideicomiso")
    List<SolicitudCuenta> findByFideicomisoFolio(@Param("folioFideicomiso") String folioFideicomiso);
    // Cambié el nombre del método a findByFideicomisoFolio para ser más conciso,
    // pero la clave es el @Query

    // Nuevo método para encontrar solicitudes por una lista de CVEs de región
    @Query("""
            SELECT sc
            FROM SolicitudCuenta sc
            JOIN InstruccionMonetaria im
                 ON im.instruccion = sc.instruccion
            WHERE im.fechaAprobacion IS NOT NULL
              AND sc.fideicomiso_folio.region.cve IN :cveRegiones
            """)
    List<SolicitudCuenta> findByFideicomisoRegionCveIn(@Param("cveRegiones") List<String> cveRegiones);

}