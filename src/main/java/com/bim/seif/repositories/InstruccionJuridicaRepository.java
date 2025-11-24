package com.bim.seif.repositories;

import com.bim.seif.models.Instruccion;
import com.bim.seif.models.InstruccionJuridica;
import com.bim.seif.models.dto.InstruccionJuridicaDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.persistence.Tuple;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public interface InstruccionJuridicaRepository extends JpaRepository<InstruccionJuridica, String> {

    List<Instruccion> findByResponsable(@Param("email") String email);

    @Query(value = """
            SELECT
                i.folio,  -- 0  String
                f.alias,  -- 1  String
                r.descripcion,      -- 2  String
                i.ruta_archivo,     -- 3  String
                i.comentario,       -- 4  String
                'Juridica' as tipo, -- 5  String
                i.fecha_alta,       -- 6  Timestamp (usa este para fechaModificacion)
                ij.responsable,     -- 7  String
                i.cliente_carga,    -- 8  String
                CONCAT(c.nombre, ' ', c.apellido_paterno, ' ', c.apellido_materno) as nombre_cliente, -- 9 String
                ij.estatus_cve,      -- 10 estatus
                ij.solicitud_correccion -- 11 solicitud_correccion
            FROM instruccion i
            INNER JOIN instruccion_juridica ij ON i.folio = ij.folio
            LEFT JOIN fideicomiso f ON f.folio = i.fideicomiso_folio
            LEFT JOIN region r ON r.cve = f.region_cve
            LEFT JOIN cliente c ON c.email = f.nombre_cliente
            WHERE ij.responsable = :email AND ij.estatus_cve <> 'FI'
            """, nativeQuery = true)
    List<Tuple> findInstruccionesJuridicasTuples(@Param("email") String email);

    default List<InstruccionJuridicaDto> findInstruccionesJuridicas(String email) {
        List<Tuple> tuples = findInstruccionesJuridicasTuples(email);
        return tuples.stream().map(tuple -> {
            InstruccionJuridicaDto dto = new InstruccionJuridicaDto();
            dto.setFolio(tuple.get(0, String.class));// i.folio
            dto.setAliasFideicomiso(tuple.get(1, String.class));// i.Alias
            dto.setRegionDesc(tuple.get(2, String.class));//r.descripcion,
            dto.setRutaArchivo(tuple.get(3, String.class));
            dto.setComentario(tuple.get(4, String.class));
            dto.setTipoInstruccion(tuple.get(5, String.class));
            dto.setFechaHoraAlta(tuple.get(6, Timestamp.class));
            dto.setResponsable(tuple.get(7, String.class));
            dto.setCorreoClienteInstruccion(tuple.get(8, String.class));
            dto.setClienteInstruccion(tuple.get(9, String.class));  
            dto.setFechaModificacion(tuple.get(6, Timestamp.class));  // i.fecha_alta -> lo usamos como fechaModificacion
            dto.setInstruccion(null);
            dto.setUrgente(false);
            dto.setOperadaParcialmente(false);
            dto.setEstatusCve(tuple.get(10, String.class));
            dto.setSolicitudCorreccion(tuple.get(11, Boolean.class));

            return dto;
        }).collect(Collectors.toList());
    }

    Optional<InstruccionJuridica> findByInstruccionFolio(String instruccionFolio);

    Optional<InstruccionJuridica> findByInstruccionFolioAndSolicitudCorreccionIsFalse(String instruccionFolio);


    //Retorna operaciones con solicitud de archivos
    @Query(value = """
                SELECT
                ij.folio,
                 ij.fecha_modificacion,
                 ij.operada_parcialmente,
                 ij.urgente,
                 ij.responsable,
                 ij.estatus_cve as instruccionestatus_cve,
                 ij.responsable_email,
                 ij.fecha_envio_aprobacion,
                 oj.id as id_oj,
                 oj.fecha_registro,
                 oj.instruccion_folio,
                 oj.tipo_operacion_cve,
                 oj.descripcion_operacion,
                 oj.comentario,
                 oj.observaciones,
                 oj.cliente_correo_electronico,
                 oj.instruccion_cumple_fines,
                 oj.bool_cliente_email,
                 oj.firmas_correctas,
                 oj.estatus_cve as operacionestatus_cve,
                 sd.id as id_sd,
                 sd.id_operacion as id_operacion_sd,
                 sd.fecha_solicitud,
                 sd.fecha_carga,
                 sd.nombre_archivo,
                 sd.ruta_archivo,
                 sd.descripcion_del_acto,
                 sd.nombre_formato,
                 sd.ruta_formato,
                 sd.nota
             FROM instruccion_juridica ij
             INNER JOIN operacion_juridica oj ON ij.folio = oj.instruccion_folio
             LEFT JOIN solicitud_documento_operacion_juridica sd ON sd.id_operacion = oj.id
             WHERE ij.folio = :instruccionFolio
             ORDER BY oj.id
            """, nativeQuery = true)
    Optional<List<Map<String, Object>>> findByOperacionesFolio_SolicitudArchivos(String instruccionFolio);

    @Modifying
    @Query("delete from InstruccionJuridica i where i.folio = :fol")
    void borrar(@Param("fol") String instruccionFolio);

    List<InstruccionJuridica> findByInstruccionFideicomisoRegionCveIn(List<String> regionCve);

    List<InstruccionJuridica> findByResponsableAndEstatusCveNotLike(String email, String Status);

    @Query("""
      select count(ij) 
      from InstruccionJuridica ij 
      where ij.estatus.cve in ('PR','PE')
        and lower(ij.responsable) = lower(?1)
    """)
    long countPendientesOEnProcesoByEmail(String email);

        @Query("""
      select j.folio
      from InstruccionJuridica j
      where lower(j.responsable) = lower(:email)
        and j.estatus.cve in ('PR','PE')
    """)
    List<String> foliosJuridicas(@Param("email") String email);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE InstruccionJuridica ij
           SET ij.responsable = :nuevo
         WHERE ij.folio IN :folios
           AND ij.estatus IN ('PR','PE')
    """)
    int reasignarResponsableJuridicas(@Param("folios") List<String> folios,
                                      @Param("nuevo") String nuevo);
}
