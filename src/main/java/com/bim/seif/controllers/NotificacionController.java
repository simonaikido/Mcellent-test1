package com.bim.seif.controllers;

import com.bim.seif.models.TipoNotificacion;
import com.bim.seif.models.TipoPropiedad;
import com.bim.seif.models.dto.NotificacionDto;
import com.bim.seif.services.AlertaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/notificaciones")
@RequiredArgsConstructor
public class NotificacionController {

    private final AlertaService notificacionService;

    @PutMapping("/switch/{cve}")
    public ResponseEntity<Void> cambiarEstatus(@PathVariable TipoNotificacion cve){
        log.info("solicitud de cambio de estatus {}", cve);
        notificacionService.cambiarEstatus(cve);
        log.info("estatus de notificacion {} cambiado con exito", cve);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> actualizarNotificacion(@PathVariable TipoNotificacion id, @RequestBody NotificacionDto notificacionDto){

        log.info("Solicitud de actualizacion de notificación para : {}", id);
        notificacionService.actualizarNotificacion(id, notificacionDto);
        log.info("Notificación {} actualizada con exito.", id);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<NotificacionDto>> obtenerNotificaciones(){
        log.info("Solicitud de obtencion de todas las notificaciones.");
        List<NotificacionDto> notificaciones = notificacionService.obtenerNotificaciones();
        log.info("Se recuperaron {} notificaciones.", notificaciones.size());
        return ResponseEntity.ok(notificaciones);
    }

    @GetMapping("/{cve}/parametros")
    public ResponseEntity<Set<Map<TipoPropiedad, Map<String,String>>>> obtenerVariablesPorEvento(@PathVariable TipoNotificacion cve){
         log.info("Solicitud de obtencion de parametros para: {}", cve);
        
        Set<Map<TipoPropiedad, Map<String,String>>> parametros = notificacionService.obtenerParametro(cve);
        
        log.info("Se recuperaron {} conjuntos de parametros para: {}", parametros.size(), cve);
        return ResponseEntity.ok(notificacionService.obtenerParametro(cve));
    }

}
