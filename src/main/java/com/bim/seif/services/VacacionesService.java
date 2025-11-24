package com.bim.seif.services;

import com.bim.seif.models.dto.AsignacionRes;
import com.bim.seif.models.dto.CrearAsignacionReq;
import com.bim.seif.models.VacacionesAsignacion;
import com.bim.seif.models.EstadoAsignacion;
import com.bim.seif.repositories.VacacionesAsignacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class VacacionesService {

    private final VacacionesAsignacionRepository asignacionRepo;

    @Transactional
    public AsignacionRes crearAsignacion(CrearAsignacionReq req, String creadoPor) {
        if (req.getTitularEmail().equalsIgnoreCase(req.getSustitutoEmail())) {
            throw new IllegalArgumentException("El sustituto no puede ser el mismo que el titular.");
        }
        if (req.getFin().isBefore(req.getInicio())) {
            throw new IllegalArgumentException("La fecha fin debe ser posterior o igual a la fecha inicio.");
        }

        var inicioUTC = VacacionesAsignacion.startOfDayUTC(req.getInicio());
        var finUTC    = VacacionesAsignacion.endOfDayUTC(req.getFin());

        var entidad = VacacionesAsignacion.builder()
                .titularEmail(req.getTitularEmail().trim().toLowerCase())
                .sustitutoEmail(req.getSustitutoEmail().trim().toLowerCase())
                .fechaInicio(inicioUTC)
                .fechaFin(finUTC)
                .estado(EstadoAsignacion.ACTIVA)
                .autoRevert(req.getAutoRevert() == null ? true : req.getAutoRevert())
                .desactivadoTitular(req.getDesactivarTitular() != null && req.getDesactivarTitular())
                .creadoPor(creadoPor)
                .motivo(req.getMotivo())
                .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                .build();

        entidad = asignacionRepo.save(entidad);

        return AsignacionRes.builder()
                .id(entidad.getId())
                .titularEmail(entidad.getTitularEmail())
                .sustitutoEmail(entidad.getSustitutoEmail())
                .fechaInicio(entidad.getFechaInicio())
                .fechaFin(entidad.getFechaFin())
                .estado(entidad.getEstado())
                .autoRevert(entidad.isAutoRevert())
                .desactivadoTitular(entidad.isDesactivadoTitular())
                .creadoPor(entidad.getCreadoPor())
                .motivo(entidad.getMotivo())
                .createdAt(entidad.getCreatedAt())
                .build();
    }
}