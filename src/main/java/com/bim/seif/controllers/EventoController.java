package com.bim.seif.controllers;

import com.bim.seif.models.TipoEvento;
import com.bim.seif.models.TipoPropiedad;
import com.bim.seif.models.dto.EventoDto;
import com.bim.seif.services.EventoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/eventos")
@RequiredArgsConstructor
public class EventoController {

    private final EventoService eventoService;

    @PutMapping("/switch/{cve}")
    public ResponseEntity<Void> cambiarEstatus(@PathVariable TipoEvento cve) {
        
        
        eventoService.cambiarEstatus(cve);
        
        log.info("Estatus de TipoEvento {} cambiado con exito.", cve);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{cve}")
    public ResponseEntity<Void> actualizarEvento(@PathVariable TipoEvento cve, @RequestBody EventoDto evento) {
  
        eventoService.actualizarEvento(cve, evento);
        
        log.info("Evento {} actualizado con exito.", cve);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<?> obtenerEventos(@AuthenticationPrincipal Jwt jwt) {
        
        String usuario = jwt != null ? jwt.getSubject() : "anonimo";

        
        try {
            // Validación del token (puede ser útil para logs o auditoría)
            System.out.println("Usuario autenticado: " + usuario + " | Accediendo a /eventos");

            List<EventoDto> eventos = eventoService.obtenerEventos();

            if (eventos == null || eventos.isEmpty()) {
                log.info("No hay eventos registrados. Usuario: {}", usuario);
                return ResponseEntity.status(HttpStatus.NO_CONTENT)
                        .body("No hay eventos registrados actualmente.");
            }

            log.info("Se recuperaron {} eventos. Usuario: {}", eventos.size(), usuario);
            return ResponseEntity.ok(eventos);

        } catch (Exception e) {
            e.printStackTrace();
            log.error("Error al obtener los eventos. Usuario: {}. Causa: {}", usuario, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al obtener los eventos: " + e.getMessage());
        }
    }

    @GetMapping("/{cve}/parametros")
    public ResponseEntity<Set<Map<TipoPropiedad, Map<String, String>>>> obtenerVariablesPorEvento(
            @PathVariable TipoEvento cve) {

        Set<Map<TipoPropiedad, Map<String, String>>> parametros = eventoService.obtenerParametros(cve);
        
        log.info("Se recuperaron {} conjuntos de parametros para TipoEvento: {}", parametros.size(), cve);
        return ResponseEntity.ok(parametros);
    }

}