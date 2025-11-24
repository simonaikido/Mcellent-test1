package com.bim.seif.services;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bim.seif.models.EstadoAsignacion;
import com.bim.seif.models.VacacionesAsignacion;
import com.bim.seif.models.VacacionesFolioLog;
import com.bim.seif.repositories.InstruccionJuridicaRepository;
import com.bim.seif.repositories.InstruccionMonetariaRepository;
import com.bim.seif.repositories.VacacionesAsignacionRepository;
import com.bim.seif.repositories.VacacionesFolioLogRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class VacacionesApplyService {

    private final VacacionesAsignacionRepository asignacionRepo;
    private final VacacionesFolioLogRepository logRepo;
    private final InstruccionMonetariaRepository monetariaRepo;
    private final InstruccionJuridicaRepository juridicaRepo;

    @Transactional
    public int aplicarReasignacion(Long asignacionId) {
        VacacionesAsignacion asig = asignacionRepo.findById(asignacionId)
                .orElseThrow(() -> new IllegalArgumentException("Asignación no encontrada"));

        // (opcional) validaciones de ventana de fechas/estado
        // if (asig.getFechaInicio().isAfter(OffsetDateTime.now())) { ... }

        List<VacacionesFolioLog> pendientes = logRepo.findByAsignacionIdAndValorNuevo(asignacionId, "PENDIENTE");
        if (pendientes.isEmpty())
            return 0;

        final String sustituto = asig.getSustitutoEmail();
        final OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        // Agrupar por tipo
        List<String> foliosMon = new ArrayList<>();
        List<String> foliosJur = new ArrayList<>();
        pendientes.forEach(l -> {
            switch (l.getTipo()) {
                case MONETARIA -> foliosMon.add(l.getFolio());
                case PROGRAMADA -> foliosMon.add(l.getFolio()); // si programada está en la misma entidad
                case JURIDICA -> foliosJur.add(l.getFolio());
            }
        });

        int totalUpdates = 0;
        if (!foliosMon.isEmpty()) {
            totalUpdates += monetariaRepo.reasignarResponsableMonetarias(foliosMon, sustituto);
        }
        if (!foliosJur.isEmpty()) {
            totalUpdates += juridicaRepo.reasignarResponsableJuridicas(foliosJur, sustituto);
        }

        // Marca logs como aplicados (valor_nuevo = sustituto)
        List<Long> ids = pendientes.stream().map(VacacionesFolioLog::getId).toList();
        logRepo.marcarAplicados(ids, sustituto, now);

        // (opcional) actualizar estado de la asignación
        if (asig.getEstado() == EstadoAsignacion.PROGRAMADA) {
            asig.setEstado(EstadoAsignacion.ACTIVA);
        }
        asignacionRepo.save(asig);

        return totalUpdates;
    }
}
