package com.bim.seif.controllers;

import com.bim.seif.models.dto.AsignacionRes;
import com.bim.seif.models.dto.CrearAsignacionReq;
import com.bim.seif.services.VacacionesService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/vacaciones/asignaciones")
@RequiredArgsConstructor
public class VacacionesController {

    private final VacacionesService service;

    @PostMapping
    public ResponseEntity<AsignacionRes> crear(@Validated @RequestBody CrearAsignacionReq req,
                                               Principal principal) {
        String creadoPor = principal != null ? principal.getName() : "system";
        var res = service.crearAsignacion(req, creadoPor);
        return ResponseEntity.ok(res);
    }
}