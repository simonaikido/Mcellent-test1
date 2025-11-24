package com.bim.seif.controllers;

import com.bim.seif.models.Actividad;
import com.bim.seif.models.dto.ActividadDto;
import com.bim.seif.models.dto.ActividadRequest;
import com.bim.seif.services.ActividadService;
import com.bim.seif.utils.LogsUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

@Slf4j
@RestController
@RequestMapping("/actividades")
@RequiredArgsConstructor
public class ActividadController {

    private final ActividadService actividadService;
    @Autowired
    private LogsUtils logsUtils;

    @GetMapping
    public ResponseEntity<List<ActividadDto>> getActividadesByFolioInstruccion(HttpServletRequest request,
            @RequestParam String folioInstruccion) {
        log.info("Solicitud de actividades para folioInstruccion: {}", folioInstruccion);
        // El servicio ahora devuelve DTOs directamente
        List<ActividadDto> actividades = actividadService.obtenerActividadesPorFolioInstruccion(folioInstruccion);
        log.info(logsUtils.logUserAndIpFromHead(request));
        log.info("Encontradas {} actividades para folioInstruccion: {}", actividades.size(), folioInstruccion);
        return ResponseEntity.ok(actividades);
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> registrarActividad(@RequestBody ActividadRequest actividad) {
        log.info("Intento de registrar actividad para folioInstruccion: {}", actividad.getFolioInstruccion());
        if (actividad.getFolioInstruccion() == null
                || actividad.getDescripcion() == null
                || actividad.getUsuario() == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Campos requeridos: folioInstruccion, descripcion, usuario"));
        }
        Actividad nueva = actividadService.registrarActividad(
                actividad.getFolioInstruccion(),
                actividad.getDescripcion(),
                actividad.getUsuario());

        if (nueva == null) {

            log.error("Fallo al registrar actividad. Instruccion no encontrada para folio: {}", actividad.getFolioInstruccion());

            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Instruccion no encontrada"));
        }

        Map<String, Object> body = new java.util.HashMap<>();
        body.put("id", nueva.getId());
        body.put("fechaHora", nueva.getFechaHora()); // usa HashMap, no Map.of, por si viene null
        body.put("folioInstruccion", actividad.getFolioInstruccion());
        body.put("mensaje", "Actividad registrada");


        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }
}