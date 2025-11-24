package com.bim.seif.repositories;

import com.bim.seif.models.SolicitudArchivoJuridica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SolicitudArchivoJuridicaRepository extends JpaRepository<SolicitudArchivoJuridica, Long> {

    @Query(value = """
        SELECT sdoj.*
                 FROM solicitud_documento_operacion_juridica sdoj
                 WHERE sdoj.id_operacion = :id
    """, nativeQuery = true)
    Optional<List<SolicitudArchivoJuridica>> findAllSolicitudesPorOperacion(@Param("id") Long id);
}
