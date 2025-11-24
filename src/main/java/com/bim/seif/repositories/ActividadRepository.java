package com.bim.seif.repositories;

import com.bim.seif.models.Actividad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActividadRepository extends JpaRepository<Actividad, Long> {

    List<Actividad> findByInstruccion_FolioOrderByFechaHoraDesc(String instruccionFolio);
}