package com.bim.seif.controllers;

import com.bim.seif.models.SolicitudTipoCambio;
import com.bim.seif.models.dto.PactarTipoCambioRequest;
import com.bim.seif.models.dto.SolicitudCuentaResumenDto;
import com.bim.seif.models.dto.SolicitudTipoCambioDto;
import com.bim.seif.services.SolicitudTipoCambioService;
import com.bim.seif.utils.Validators;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/solicitud-tipo-cambio")
public class SolicitudTipoCambioController {

    @Autowired
    private SolicitudTipoCambioService solicitudTipoCambioService;

    @GetMapping
    public ResponseEntity<List<SolicitudTipoCambioDto>> obtenerSolicitudes() {
        List<SolicitudTipoCambioDto> lista = solicitudTipoCambioService.obtenerTodasLasSolicitudes();
        return ResponseEntity.ok(lista);
    }

    @PostMapping("/solicitudes")
    public ResponseEntity<List<SolicitudTipoCambioDto>> obtenerSolicitudes(
            @RequestBody SolicitudTipoCambioDto request,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate desde,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate hasta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "id") String sortBy) {

        // Obtener el usuario (correo o uid) desde el token JWT
        String username = jwt.getSubject();
        Validators.sanitizeUsername(username);

        // Validaciones opcionales
        if (desde != null && hasta != null && hasta.isBefore(desde)) {
            return ResponseEntity.badRequest().body(null);
        }

        // Crear el pageable
        var pageable = PageRequest.of(page, size, Sort.by(sortBy));

        // Obtener las solicitudes desde el servicio
        List<SolicitudTipoCambioDto> solicitudes = solicitudTipoCambioService.obtenerSolicitudes(username, request,
                desde, hasta, pageable);

        return ResponseEntity.ok(solicitudes);
    }

    @PutMapping("/rechazar/{id}")
    public ResponseEntity<Void> rechazarSolicitud(@PathVariable Long id) {
        solicitudTipoCambioService.rechazar(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping
    public ResponseEntity<SolicitudTipoCambio> crear(@RequestBody SolicitudTipoCambio solicitud) {
        SolicitudTipoCambio nueva = solicitudTipoCambioService.crearSolicitudTipoCambio(solicitud);
        return ResponseEntity.status(HttpStatus.CREATED).body(nueva);
    }

    @GetMapping("/{folio}")
    public List<SolicitudCuentaResumenDto> obtenerPorFolio(@PathVariable String folio) {
        return solicitudTipoCambioService.obtenerPorFolio(folio);
    }

    @PutMapping("/{id}/pacto/{validadorEmail}")
    public ResponseEntity<Void> actualizarPacto(
            @PathVariable Long id,
            @RequestBody PactarTipoCambioRequest req,
            @PathVariable String validadorEmail) {
        solicitudTipoCambioService.actualizarPacto(id, req, validadorEmail); // ← sin mapper
        return ResponseEntity.noContent().build(); // 204
    }

}