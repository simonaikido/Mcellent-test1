package com.bim.seif.repositories;

import com.bim.seif.models.InstruccionClasificada;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface InstruccionClasificadaRepository  { // extends JpaRepository<InstruccionClasificada, Long>
    List<InstruccionClasificada> findByEstatus(String estatus);

    List<InstruccionClasificada> findByTipoInstruccion(String tipoInstruccion);
    List<InstruccionClasificada> findByRegionFideicomiso(String regionFideicomiso);

    //Filtrar por rango de fechas
    List<InstruccionClasificada> findByFechaHoraRegistroBetween(LocalDateTime fechaInicio, LocalDateTime fechaFin);

    //Filtrar por empleado responsable (necesitas el campo en InstruccionClasificada o en OperacionInstruccion)
    @Query("SELECT ic FROM InstruccionClasificada ic JOIN ic.operaciones op WHERE op.empleadoResponsable = :empleadoResponsable")
    List<InstruccionClasificada> findByEmpleadoResponsable(@Param("empleadoResponsable") String empleadoResponsable);

    List<InstruccionClasificada> findByNombreFideicomisoContainingIgnoreCase(String nombreFideicomiso);

    //Ordenar por fecha de registro
    List<InstruccionClasificada> findAllByOrderByFechaHoraRegistroAsc();
}