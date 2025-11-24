package com.bim.seif.repositories;

import com.bim.seif.models.InstruccionMonetaria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface InstruccionMonetariaRepository extends JpaRepository<InstruccionMonetaria, String> {

        List<InstruccionMonetaria> findByResponsableAndEstatusCve(String email, String clave);

        List<InstruccionMonetaria> findByResponsable(String email);

        @Modifying
        @Query("delete from InstruccionMonetaria i where i.folio = :fol")
        void borrar(@Param("fol") String instruccionFolio);

        // Método para encontrar InstruccionMonetaria por el folio de la Instruccion
        // asociada
        Optional<InstruccionMonetaria> findByInstruccionFolio(String instruccionFolio);

        List<InstruccionMonetaria> findByResponsableAndEstatusCveIsNot(String email, String claveExcluir);

        List<InstruccionMonetaria> findByValidadorEmail(String validadorEmail);

        List<InstruccionMonetaria> findByMesaControlTrue();

        List<InstruccionMonetaria> findByFechaAprobacionIsNotNull();

        @Query("SELECT DISTINCT im FROM InstruccionMonetaria im " +
                        "LEFT JOIN im.operaciones op " +
                        "LEFT JOIN op.comprobante comp " + // <-- agregado
                        "WHERE im.responsable = :email " +
                        "AND (op.id IS NULL " + // No tienen operaciones registradas
                        "     OR im.fechaAprobacion IS NOT NULL " + // Tiene aprobación
                        "     OR im.solicitudCorreccion = TRUE) " + // Se solicitó corrección
                        "AND im.instruccion.fechaAtencion IS NULL " +
                        "AND im.fechaCancelacion IS NULL " + // Filtra canceladas
                        "AND im.estatus <> 'RE' " +
                        "AND im.estatus <> 'FI' " +
                        "ORDER BY im.fechaClasificacion DESC")
        List<InstruccionMonetaria> findMonetariasByEmpleadoEmailAndEstatusCveAndNoOperations(
                        @Param("email") String email);

        List<InstruccionMonetaria> findByInstruccionFideicomisoRegionCveIn(Set<String> region);

        // ¡METODO PARA consulta-instruccion!
        @Query("SELECT im FROM InstruccionMonetaria im " +
                        "WHERE im.responsable = :email " +
                        " AND (" +
                        "  im.estatus.cve <> :estatusPr OR " + // Que el estatus NO sea PR
                        "  (im.estatus.cve = :estatusPr AND SIZE(im.operaciones) > 0)" + // O que sea PR Y tenga
                                                                                         // operaciones
                        ")")
        List<InstruccionMonetaria> findMonetariasForConsulta(@Param("email") String email,
                        @Param("estatusPr") String estatusPr);

        @Query("SELECT im FROM InstruccionMonetaria im " +
                        "WHERE im.responsable IS NULL " +
                        "  AND im.estatus.cve = :estatusRe")
        List<InstruccionMonetaria> findRechazadasSinResponsable(@Param("estatusRe") String estatusRe);

        List<InstruccionMonetaria> findByValidadorEmailAndFechaAprobacionIsNull(String validadorEmail);

        @Query("""
                          select count(im)
                          from InstruccionMonetaria im
                          where im.estatus.cve in ('PR','PE')
                            and (
                                 lower(im.responsable) = lower(?1)
                              or lower(im.validadorEmail) = lower(?1)
                            )
                        """)
        long countPendientesOEnProcesoByEmail(String email);

        @Query("""
      select m.folio
      from InstruccionMonetaria m
      where ( lower(m.responsable) = lower(:email)
              or lower(m.validadorEmail) = lower(:email) )
        and m.estatus.cve in ('PR','PE')
        and (m.programada = false or m.programada is null)
    """)
    List<String> foliosMonetariasNoProgramadas(@Param("email") String email);

    @Query("""
      select m.folio
      from InstruccionMonetaria m
      where ( lower(m.responsable) = lower(:email)
              or lower(m.validadorEmail) = lower(:email) )
        and m.estatus.cve in ('PR','PE')
        and m.programada = true
    """)
    List<String> foliosMonetariasProgramadas(@Param("email") String email);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE InstruccionMonetaria im
           SET im.responsable = :nuevo
         WHERE im.folio IN :folios
           AND im.estatus IN ('PR','PE')
    """)
    int reasignarResponsableMonetarias(@Param("folios") List<String> folios,
                                       @Param("nuevo") String nuevo);
}