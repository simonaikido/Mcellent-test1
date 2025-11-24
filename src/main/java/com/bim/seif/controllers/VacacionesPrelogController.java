package com.bim.seif.controllers;

import com.bim.seif.models.dto.VacacionesPrelogRequest;
import com.bim.seif.models.dto.VacacionesPrelogResponse;
import com.bim.seif.services.VacacionesPrelogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/vacaciones/asignaciones/{asignacionId}/prelog")
public class VacacionesPrelogController {

    private final VacacionesPrelogService service;

    /**
     * Crea “registros” (pre-log) en vacaciones_folio_log para la asignación.
     * No realiza updates de folios aún; solo deja el plan.
     */
    @PostMapping
    public ResponseEntity<VacacionesPrelogResponse> crearRegistros(
            @PathVariable Long asignacionId,
            @RequestBody VacacionesPrelogRequest req) {

        VacacionesPrelogResponse resp = service.prelog(asignacionId, req);
        return ResponseEntity.ok(resp);
    }
}