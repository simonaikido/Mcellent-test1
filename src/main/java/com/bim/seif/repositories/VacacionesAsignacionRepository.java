package com.bim.seif.repositories;

import com.bim.seif.models.VacacionesAsignacion;
import com.bim.seif.models.EstadoAsignacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VacacionesAsignacionRepository extends JpaRepository<VacacionesAsignacion, Long> {
    List<VacacionesAsignacion> findByTitularEmailAndEstado(String titularEmail, EstadoAsignacion estado);
}
