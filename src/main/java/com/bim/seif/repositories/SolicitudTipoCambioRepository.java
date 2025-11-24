package com.bim.seif.repositories;

import com.bim.seif.models.SolicitudTipoCambio;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SolicitudTipoCambioRepository extends JpaRepository<SolicitudTipoCambio, Long>, JpaSpecificationExecutor<SolicitudTipoCambio> {
  // Aquí puedes agregar métodos personalizados si los necesitas después
  @Query("""
      SELECT DISTINCT s
      FROM SolicitudTipoCambio s
      JOIN s.operacion o
      JOIN o.instruccion i
      LEFT JOIN FETCH s.divisaPago
      LEFT JOIN FETCH s.divisaCompra
      WHERE s.fechaRechazo IS NULL
        AND s.fechaFinalizacion IS NULL
        AND i.fechaAprobacion IS NOT NULL
      """)
  List<SolicitudTipoCambio> findAllNoRechazadas();

  @Query(value = """
      SELECT
          sc.contacto AS contacto,
          sc.cuenta AS cuenta,
          sc.banco  AS banco,
          sc.beneficiario AS beneficiario,
          sc.rfc AS rfc,
          sc.direccion AS direccion,
          sc.divisa_cve AS divisaCve,
          d.nacional AS nacional,
          d.tipo_cambio_moneda_nacional AS tipoCambio,
          d.descripcion AS descripcion
      FROM solicitud_cuenta sc
      JOIN divisa d ON sc.divisa_cve = d.cve
      WHERE sc.fideicomiso_folio = :folio
        AND sc.fecha_finalizacion IS NULL
      ORDER BY sc.fecha_solicitud DESC
      """, nativeQuery = true)
  List<Object[]> findSolicitudesByFolio(@Param("folio") String folio);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("update SolicitudTipoCambio s " +
      "set s.contacto = :contacto, " +
      "    s.claveLlamada = :clave, " +
      "    s.tipoCambio = :tc " +
      "where s.id = :id")
  int updateCamposPacto(@Param("id") Long id,
      @Param("contacto") String contacto,
      @Param("clave") String claveLlamada,
      @Param("tc") int tipoCambio);

  @Query("select s from SolicitudTipoCambio s where s.id = :id")
  Optional<SolicitudTipoCambio> readForCheck(@Param("id") Long id);
}