package com.bim.seif.controllers;

import com.bim.seif.models.dto.SolicitudCuentaDto;
import com.bim.seif.models.dto.SolicitudCuentaResponseDto;
import com.bim.seif.models.dto.SolicitudCuentaResumenDto;
import com.bim.seif.services.SolicitudCuentaService;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/alta-cuenta/")
public class SolicitudCuentaController {

    @Autowired
    private SolicitudCuentaService solicitudCuentaService;

    // Endpoint POST para crear solicitud de cuenta
    @PostMapping("/{folio_instruccion}")
    public ResponseEntity<?> crearSolicitudCuenta(
            @PathVariable("folio_instruccion") String folioInstruccion,
            @RequestBody SolicitudCuentaDto solicitudCuentaDTO) {
                log.info("Solicitud para crear una nueva solicitud de cuenta para el folio de instruccion: {}", folioInstruccion);
                
                if (folioInstruccion == null || folioInstruccion.isBlank()) {
                    throw new IllegalArgumentException("Folio de instrucción inválido");
                }
                
                folioInstruccion = folioInstruccion.trim();
       
            SolicitudCuentaResponseDto nuevaSolicitudDto = solicitudCuentaService.crearSolicitudCuenta(folioInstruccion,solicitudCuentaDTO);
            return new ResponseEntity<>(nuevaSolicitudDto, HttpStatus.CREATED);
       
    }

    // Nuevo endpoint GET para recuperar solicitudes por folio de fideicomiso
    @GetMapping("/solicitudes/{folio_fideicomiso}")
    public ResponseEntity<?> getSolicitudesPorFideicomiso(@PathVariable("folio_fideicomiso") String folioFideicomiso) {

        try {
            List<SolicitudCuentaResponseDto> solicitudes = solicitudCuentaService
                    .getSolicitudesPorFideicomiso(folioFideicomiso);
            // if (solicitudes.isEmpty()) {
            // return new ResponseEntity<>("No se encontraron solicitudes de cuenta para el
            // fideicomiso con folio: " + folioFideicomiso, HttpStatus.NOT_FOUND);
            // }
            return new ResponseEntity<>(solicitudes, HttpStatus.OK);
        } catch (Exception e) {
            return new ResponseEntity<>(
                    "Error interno del servidor al recuperar las solicitudes de cuenta: " + e.getMessage(),
                    HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // GET para recuperar solicitudes por lista de CVEs de región
    @GetMapping("/solicitudes")
    public ResponseEntity<?> getSolicitudesPorRegionCve(
            @AuthenticationPrincipal(expression = "claims['sub']") String usuario,
            @RequestParam("cveRegiones") List<String> cveRegiones) {
                log.info("Solicitud para obtener solicitudes de cuenta para las regiones con CVEs: {}", cveRegiones);
        try {
            System.out.println("Usuario autenticado: " + usuario);
            System.out.println("Petición a /solicitudes recibida para regiones: " + cveRegiones);
            List<SolicitudCuentaResponseDto> solicitudes = solicitudCuentaService
                    .getSolicitudesPorRegionCve(cveRegiones);
            if (solicitudes.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("No se encontraron solicitudes de cuenta para las regiones con CVEs: " + cveRegiones);
            }
            return ResponseEntity.ok(solicitudes);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error interno del servidor al recuperar las solicitudes: " + e.getMessage());
        }
    }

    // Rechazar solicitud
    @PutMapping("/rechazar/{idSolicitud}")
    public ResponseEntity<?> rechazarSolicitudCuenta(@PathVariable Long idSolicitud) {
    	
    	if (idSolicitud == null || idSolicitud <= 0) {
            return ResponseEntity.badRequest().body("ID inválido");
        }
    	
        try {
            solicitudCuentaService.rechazarSolicitud(idSolicitud);
            Map<String, Object> response = new HashMap<>();
            response.put("mensaje", "Solicitud rechazada correctamente.");
            response.put("id", idSolicitud);
            log.info("Solicitud de cuenta rechazada con éxito para el ID: {}", idSolicitud);
            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return new ResponseEntity<>("Error al rechazar la solicitud", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PutMapping(value = "/aceptada/{idSolicitud}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> aceptadaSolicitudCuenta(@PathVariable Long idSolicitud,
            @RequestBody SolicitudCuentaResumenDto payload) {
                log.info("Solicitud para aceptar la solicitud de cuenta con ID: {}", idSolicitud);
        if (idSolicitud == null || idSolicitud <= 0) {
            return ResponseEntity.badRequest().body("ID inválido");
        }
        
        try {
            solicitudCuentaService.aceptadaSolicitud(idSolicitud, payload); // <- pasa el objeto completo
            Map<String, Object> response = new HashMap<>();
            response.put("mensaje", "Solicitud aceptada correctamente.");
            response.put("id", idSolicitud);
            log.info("Solicitud de cuenta aceptada con exito para el ID: {}", idSolicitud);
            return ResponseEntity.ok(response);
        } catch (ResponseStatusException e) {
            return new ResponseEntity<>(e.getReason(), e.getStatus());
        } catch (Exception e) {
            log.error("Error al aceptar la solicitud de cuenta con ID: {}: {}", idSolicitud, e.getMessage());
            return new ResponseEntity<>("Error al aceptar la solicitud", HttpStatus.INTERNAL_SERVER_ERROR);

        }
    }

    @PutMapping("/correcion/{idSolicitud}")
    public ResponseEntity<?> correcionSolicitudCuenta(@PathVariable Long idSolicitud) {
        log.info("Solicitud para corregir la solicitud de cuenta con ID: {}", idSolicitud);
        
        if (idSolicitud == null || idSolicitud <= 0) {
            return ResponseEntity.badRequest().body("ID inválido");
        }

        try {
            solicitudCuentaService.correcionSolicitud(idSolicitud);
            Map<String, Object> response = new HashMap<>();
            response.put("mensaje", "Solicitud correcion correctamente.");
            response.put("id", idSolicitud);
            return ResponseEntity.ok(response);
        }  catch (ResponseStatusException e) {
            return new ResponseEntity<>(e.getReason(), e.getStatus());
        } catch (Exception e) {
            log.error("Error al corregir la solicitud de cuenta con ID: {}: {}", idSolicitud, e.getMessage());
            return new ResponseEntity<>("Error al correcion la solicitud", HttpStatus.INTERNAL_SERVER_ERROR);

        }
    }

}