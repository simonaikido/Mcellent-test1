package com.bim.seif.controllers;

import com.bim.seif.models.dto.RolDto;
import com.bim.seif.services.RolService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/roles")
@RequiredArgsConstructor
public class RolController {

    private final RolService rolService;

    // Listar roles
    @GetMapping
    public ResponseEntity<List<RolDto>> listar() {
        log.info("Solicitud para obtener el listado de roles.");
        return ResponseEntity.ok(rolService.listar());
    }

    // Obtener rol por CN
    @GetMapping("/{cn}")
    public ResponseEntity<RolDto> obtener(@PathVariable String cn) {
        log.info("Solicitud para obtener el rol con CN: {}", cn);
        RolDto r = rolService.obtener(cn);
        log.info("Se recupero el rol con CN: {}", cn);
        return r == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(r);
    }

    // Crear rol (requiere al menos un miembro)
    @PostMapping
    public ResponseEntity<Void> crear(@RequestBody Map<String, Object> body) {
        log.info("Solicitud para crear un nuevo rol.");
        String cn = (String) body.get("cve");
        String descripcion = (String) body.get("descripcion");
        @SuppressWarnings("unchecked")
        List<String> miembros = (List<String>) body.get("miembros"); // lista de UIDs
        rolService.crear(cn, descripcion, miembros);
        log.info("Rol creado con CN: {}", cn);
        return ResponseEntity.created(URI.create("/roles/" + cn)).build();
    }

    // Actualizar descripción
    @PutMapping("/{cn}")
    public ResponseEntity<Void> actualizarDescripcion(@PathVariable String cn, @RequestBody RolDto dto) {
        log.info("Solicitud para actualizar la descripcion del rol con CN: {}", cn);
        rolService.actualizarDescripcion(cn, dto.getDescripcion());
        log.info("Descripcion del rol con CN: {} actualizada con exito.", cn);
        return ResponseEntity.noContent().build();
    }

    // Renombrar rol (cambiar CN)
    @PatchMapping("/{cn}/rename")
    public ResponseEntity<Void> renombrar(@PathVariable String cn, @RequestParam("nuevo") String nuevoCn) {
        log.info("Solicitud para renombrar el rol de CN: {} a CN: {}", cn, nuevoCn);
        rolService.renombrar(cn, nuevoCn);
        log.info("Rol con CN: {} renombrado a CN: {} con exito.", cn, nuevoCn);
        return ResponseEntity.noContent().build();
    }

    // Eliminar rol
    @DeleteMapping("/{cn}")
    public ResponseEntity<Void> eliminar(@PathVariable String cn) {
        log.info("Solicitud para eliminar el rol con CN: {}", cn);
        rolService.eliminar(cn);
        log.info("Rol con CN: {} eliminado con exito.", cn);
        return ResponseEntity.noContent().build();
    }

    // ===== Miembros =====

    @GetMapping("/{cn}/miembros")
    public ResponseEntity<List<String>> listarMiembros(@PathVariable String cn) {
        log.info("Solicitud para obtener los miembros del rol con CN: {}", cn);
        return ResponseEntity.ok(rolService.listarMiembros(cn));
    }

    @PostMapping("/{cn}/miembros")
    public ResponseEntity<Void> agregarMiembros(@PathVariable String cn, @RequestBody List<String> uids) {
        log.info("Solicitud para agregar miembros al rol con CN: {}", cn);
        rolService.agregarMiembros(cn, uids);
        log.info("Miembros agregados al rol con CN: {}", cn);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{cn}/miembros/{uid}")
    public ResponseEntity<Void> removerMiembro(@PathVariable String cn, @PathVariable String uid) {
        log.info("Solicitud para remover miembro con UID: {} del rol con CN: {}", uid, cn);
        rolService.removerMiembro(cn, uid);
        log.info("Miembro con UID: {} removido del rol con CN: {}", uid, cn);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{cn}/miembros")
    public ResponseEntity<Void> reemplazarMiembros(@PathVariable String cn, @RequestBody List<String> uids) {
        log.info("Solicitud para reemplazar los miembros del rol con CN: {}", cn);
        rolService.reemplazarMiembros(cn, uids);
        log.info("Miembros del rol con CN: {} reemplazados con exito.", cn);
        return ResponseEntity.noContent().build();
    }
}