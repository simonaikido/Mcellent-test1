package com.bim.seif.repositories;

import com.bim.seif.models.DiasFestivos;
import com.bim.seif.models.InstruccionProgramada;
import com.bim.seif.models.dto.TipoOperacionMonetariaProgramadaDto;
import feign.Param;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.Tuple;

@Repository
public interface InstruccionProgramadaRepository extends JpaRepository<InstruccionProgramada, String> {

        @Query("SELECT ip FROM InstruccionProgramada ip " + "JOIN ip.instruccion i " + "JOIN i.fideicomiso f "
                        + "WHERE f.folio = :folioFideicomiso")
        List<InstruccionProgramada> findByInstruccionFolioFideicomiso(String folioFideicomiso);

        /*
         * @Query("SELECT ip FROM InstruccionProgamada ip " +
         * "WHERE ip.responsable_email = :responsableEmail")
         */
        @Query(value = """
                          SELECT DISTINCT
                            im.folio                                        AS folio,
                            i.fecha_alta                                    AS fechainicio,
                            f.alias                                         AS aliasFideicomiso,
                            r.descripcion                                   AS region,
                            i.ruta_archivo                                  AS archivo,
                            i.comentario                                    AS comentario,
                            ip.monto                                        AS monto,
                            ip.concepto                                     AS concepto,

                            -- cuentaCargo = cuenta - banco - divisa
                            CONCAT(
                              COALESCE(CAST(ip.cuenta_cargo AS varchar(64)), ''), ' - ',
                              COALESCE(ccc1.banco, ''), ' - ',
                              COALESCE(ccc1.divisa_cve, '')
                            )                                               AS cuentaCargo,

                            -- cuenta = cuenta - banco - beneficiario - divisa
                            CONCAT(
                              COALESCE(CAST(ip.cuenta_abono_cuenta AS varchar(64)), ''), ' - ',
                              COALESCE(cca1.banco, ''), ' - ',
                              COALESCE(cca1.beneficiario, ''), ' - ',
                              COALESCE(cca1.divisa_cve, '')
                            )                                               AS cuenta,

                            ip.tipo_operacion_monetaria_cve                 AS tipoOperacion
                          FROM instruccion i
                          INNER JOIN (SELECT DISTINCT folio FROM instruccion_monetaria) im
                                  ON im.folio = i.folio
                          INNER JOIN instruccion_programada ip
                                  ON ip.folio = i.folio
                          LEFT  JOIN fideicomiso f  ON i.fideicomiso_folio = f.folio
                          LEFT  JOIN region r       ON r.cve = f.region_cve

                          -- Cargo (tomamos 1 fila)
                          OUTER APPLY (
                             SELECT TOP 1 ccc.banco, ccc.divisa_cve
                             FROM campo_cuenta_cargo ccc
                             WHERE ccc.cuenta = CAST(ip.cuenta_cargo AS varchar(64))
                             -- ORDER BY ccc.id DESC  -- quita si no existe 'id'
                          ) ccc1

                          -- Abono (tomamos 1 fila)
                          OUTER APPLY (
                             SELECT TOP 1 cca.banco, cca.beneficiario, cca.divisa_cve
                             FROM cuenta_abono cca
                             WHERE cca.cuenta = CAST(ip.cuenta_abono_cuenta AS varchar(64))
                             -- ORDER BY cca.id DESC  -- quita si no existe 'id'
                          ) cca1

                          WHERE ip.responsable_email = :responsableEmail
                            AND i.fecha_cancelacion IS NULL
                            AND i.fecha_rechazo IS NULL
                          ORDER BY i.fecha_alta DESC
                        """, nativeQuery = true)
        List<Object[]> findByResponsableEmail(@Param("responsableEmail") String responsableEmail);

        @Query(value = """
                        SELECT pro.*
                            FROM programacion pro
                            WHERE pro.instruccion_progamada_folio = :folio
                        """, nativeQuery = true)
        List<Object[]> findByProgramacionEmail(@Param("folio") String folio);

        @Modifying
        @Transactional
        @Query(value = """
                            UPDATE instruccion_Programada
                                            SET concepto = :#{#instruccion.concepto},
                                                comentario = :#{#instruccion.comentario},
                                                cuenta_cargo = :#{#instruccion.cuentaCargo},
                                                monto = :#{#instruccion.monto},
                                                referencia = :#{#instruccion.referencia},
                                                tipo_operacion_monetaria_cve = :#{#instruccion.tipoOperacionMonetaria.cve},
                                                divisa_cve = :#{#instruccion.divisa.cve},
                                                cuenta_abono_cuenta = :#{#instruccion.cuenta}
                                            WHERE folio = :#{#instruccion.folio}
                        """, nativeQuery = true)
        void actializarInstruccionProgramada(@Param("instruccion") InstruccionProgramada instruccion);

        /*
         * @Modifying
         * 
         * @Query("DELETE FROM instruccion_programada WHERE folio = :fol")
         * void borrarProgramada(@org.springframework.data.repository.query.Param("fol")
         * String instruccionFolio);
         */

        @Query(value = """
                        SELECT
                         tom.cve,
                         tom.descripcion
                         FROM tipo_operacion_monetaria tom WHERE tom.programacion <> 0
                        """, nativeQuery = true)
        List<Tuple> findTiposOperacionProgramadaTuples();

        default List<TipoOperacionMonetariaProgramadaDto> findTiposOperacionProgramada() {
                List<Tuple> tuples = findTiposOperacionProgramadaTuples();
                return tuples.stream()
                                .map(tuple -> new TipoOperacionMonetariaProgramadaDto(tuple.get(0, String.class),
                                                tuple.get(1, String.class)))
                                .collect(Collectors.toList());
        }

        @Query("select d from DiasFestivos d")
        List<DiasFestivos> findDiasFestivos();

        @Query(value = """
                        SELECT cl.email FROM contrato co
                        INNER JOIN cliente cl ON co.cliente_id = cl.id
                        WHERE co.fideicomiso_folio = :folio
                        """, nativeQuery = true)
        List<String> obtenerCorreosFideicomiso(@Param("folio") String folio);

        @Query("""
      select count(ij) 
      from InstruccionJuridica ij 
      where ij.estatus.cve in ('PR','PE')
        and lower(ij.responsable) = lower(?1)
    """)
    long countPendientesOEnProcesoByEmail(String email);
}