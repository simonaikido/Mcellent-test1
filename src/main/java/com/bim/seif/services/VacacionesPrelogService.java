package com.bim.seif.services;

import com.bim.seif.models.*;
import com.bim.seif.models.dto.FoliosAsignadosResponse;
import com.bim.seif.models.dto.VacacionesPrelogRequest;
import com.bim.seif.models.dto.VacacionesPrelogResponse;
import com.bim.seif.repositories.VacacionesAsignacionRepository;
import com.bim.seif.repositories.VacacionesFolioLogRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class VacacionesPrelogService {

    private final VacacionesAsignacionRepository asignacionRepo;
    private final VacacionesFolioLogRepository logRepo;
    private final FoliosAsignadosService foliosAsignadosService;
    private final VacacionesApplyService applyService; // <— inyecta el servicio de apply

    @Transactional
    public VacacionesPrelogResponse prelog(Long asignacionId, VacacionesPrelogRequest req) {
        VacacionesAsignacion asignacion = asignacionRepo.findById(asignacionId)
            .orElseThrow(() -> new IllegalArgumentException("Asignación no encontrada"));

        FoliosAsignadosResponse folios = foliosAsignadosService.listarFolios(req.getTitularEmail());

        List<VacacionesFolioLog> batch = new ArrayList<>();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        for (String f : folios.getMonetarias()) {
            batch.add(VacacionesFolioLog.builder()
                .asignacion(asignacion)
                .folio(f)
                .tipo(TipoInstruccion.MONETARIA)
                .campo(CampoAsignado.RESPONSABLE)
                .valorAnterior(req.getTitularEmail())
                .valorNuevo("PENDIENTE")
                .movedAt(now)
                .build());
        }
        for (String f : folios.getProgramadas()) {
            batch.add(VacacionesFolioLog.builder()
                .asignacion(asignacion)
                .folio(f)
                .tipo(TipoInstruccion.PROGRAMADA)
                .campo(CampoAsignado.RESPONSABLE)
                .valorAnterior(req.getTitularEmail())
                .valorNuevo("PENDIENTE")
                .movedAt(now)
                .build());
        }
        for (String f : folios.getJuridicas()) {
            batch.add(VacacionesFolioLog.builder()
                .asignacion(asignacion)
                .folio(f)
                .tipo(TipoInstruccion.JURIDICA)
                .campo(CampoAsignado.RESPONSABLE)
                .valorAnterior(req.getTitularEmail())
                .valorNuevo("PENDIENTE")
                .movedAt(now)
                .build());
        }

        if (!batch.isEmpty()) {
            logRepo.saveAll(batch);
        }

        // === Nuevo: aplicar inmediatamente después del prelog (mismo TX) ===
        boolean applyNow = req.getApplyNow() == null || Boolean.TRUE.equals(req.getApplyNow());
        int updated = 0;
        if (applyNow) {
            updated = applyService.aplicarReasignacion(asignacion.getId());
        }

        return VacacionesPrelogResponse.builder()
            .asignacionId(asignacion.getId())
            .monetariasPrelog(folios.getMonetariasCount())
            .programadasPrelog(folios.getProgramadasCount())
            .juridicasPrelog(folios.getJuridicasCount())
            .updatedRows(updated)              // <— devolvemos cuántas filas se actualizaron
            .build();
    }
}