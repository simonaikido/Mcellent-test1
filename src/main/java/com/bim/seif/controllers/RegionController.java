package com.bim.seif.controllers;

import com.bim.seif.models.dto.RegionDto;
import com.bim.seif.services.RegionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/regiones")
@RequiredArgsConstructor
public class RegionController {

    private final RegionService regionService;

    // Listar regiones
    @GetMapping
    public ResponseEntity<List<RegionDto>> listar() {
        List<RegionDto> regiones = regionService.listar();
        log.info("Se recuperaron {} regiones.", regiones.size());
        return ResponseEntity.ok(regiones);
    }

    // Obtener región por CN
    @GetMapping("/{cn}")
    public ResponseEntity<RegionDto> obtener(@PathVariable String cn) {
        RegionDto r = regionService.obtener(cn);
        log.info("Se recupero la region con CN: {}", cn);
        
        return r == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(r);
    }

    // Crear región (requiere al menos un miembro)
    @PostMapping
    public ResponseEntity<Void> crear(@RequestBody Map<String, Object> body) {
        String cn = (String) body.get("cve");           // cn = "1","2","7","8"...
        String descripcion = (String) body.get("descripcion");
        @SuppressWarnings("unchecked")
        List<String> miembros = (List<String>) body.get("miembros"); // lista de UIDs
        regionService.crear(cn, descripcion, miembros);
        log.info("Region creada con CN: {}", cn);
        return ResponseEntity.created(URI.create("/regiones/" + cn)).build();
    }

    // Actualizar descripción
    @PutMapping("/{cn}")
    public ResponseEntity<Void> actualizarDescripcion(@PathVariable String cn, @RequestBody RegionDto dto) {
        log.info("Solicitud para actualizar la descripcion de la region con CN: {}", cn);
        regionService.actualizarDescripcion(cn, dto.getDescripcion());
        return ResponseEntity.noContent().build();
    }

    // Renombrar región (cambiar CN)
    @PatchMapping("/{cn}/rename")
    public ResponseEntity<Void> renombrar(@PathVariable String cn, @RequestParam("nuevo") String nuevoCn) {
        log.info("Solicitud para renombrar la region de CN: {} a CN: {}", cn, nuevoCn);
        regionService.renombrar(cn, nuevoCn);
        log.info("Region con CN: {} renombrada a CN: {} con exito.", cn, nuevoCn);
        return ResponseEntity.noContent().build();
    }

    // Eliminar región
    @DeleteMapping("/{cn}")
    public ResponseEntity<Void> eliminar(@PathVariable String cn) {
        log.info("Solicitud para eliminar la region con CN: {}", cn);
        regionService.eliminar(cn);
        log.info("Region con CN: {} eliminada con exito.", cn); 
        return ResponseEntity.noContent().build();
    }

    // ===== Miembros =====

    @GetMapping("/{cn}/miembros")
    public ResponseEntity<List<String>> listarMiembros(@PathVariable String cn) {
        log.info("Solicitud para obtener los miembros de la region con CN: {}", cn);
        return ResponseEntity.ok(regionService.listarMiembros(cn));
    }

    @PostMapping("/{cn}/miembros")
    public ResponseEntity<Void> agregarMiembros(@PathVariable String cn, @RequestBody List<String> uids) {
        log.info("Solicitud para agregar miembros a la region con CN: {}", cn);
        regionService.agregarMiembros(cn, uids);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{cn}/miembros/{uid}")
    public ResponseEntity<Void> removerMiembro(@PathVariable String cn, @PathVariable String uid) {
        log.info("Solicitud para remover miembro con UID: {} de la region con CN: {}", uid, cn);
        regionService.removerMiembro(cn, uid);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{cn}/miembros")
    public ResponseEntity<Void> reemplazarMiembros(@PathVariable String cn, @RequestBody List<String> uids) {
        log.info("Solicitud para reemplazar los miembros de la region con CN: {}", cn);
        regionService.reemplazarMiembros(cn, uids);
        return ResponseEntity.noContent().build();
    }
}