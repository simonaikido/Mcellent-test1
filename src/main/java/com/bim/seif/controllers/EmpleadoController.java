package com.bim.seif.controllers;

import java.security.Principal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

import com.bim.seif.crypto.CryptoFrontService;
import com.bim.seif.models.Propiedad;
import com.bim.seif.models.Rol;
import com.bim.seif.models.TipoEvento;
import com.bim.seif.models.dto.DeletionGuardResponse;
import com.bim.seif.models.dto.EmpleadoDto;
import com.bim.seif.models.dto.EmpleadoReporteDto;
import com.bim.seif.models.dto.FiltroUsuariosInternosDto;
import com.bim.seif.models.dto.FoliosAsignadosResponse;
import com.bim.seif.services.AuditoriaService;
import com.bim.seif.services.EmailService;
import com.bim.seif.services.EmpleadoService;
import com.bim.seif.services.FoliosAsignadosService;
import com.bim.seif.services.UserDeletionGuardService;
import com.bim.seif.utils.Validators;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/ejecutivos")
@RequiredArgsConstructor
public class EmpleadoController {

    private final EmpleadoService empleadoService;
    private final AuditoriaService auditoriaService;
    private final EmailService emailService;
    private final CryptoFrontService cryptoFrontService;
    private final UserDeletionGuardService guardService;
    private final FoliosAsignadosService service;

    @GetMapping
    public ResponseEntity<List<EmpleadoDto>> buscarEjecutivos(
            @RequestParam(name = "rol") Rol rol,
            @RequestParam(name = "region") String region) {

        List<EmpleadoDto> empleados = empleadoService.buscarEmpleados(rol.name(), region);

        log.info("Encontrados {} empleados para Rol: {} y Region: {}", empleados.size(), rol.name(), region);
        return ResponseEntity.ok(empleados);
    }

    // LISTAR GENERAL (filtro opcional por uid/cn)
    @GetMapping("/listar")
    public ResponseEntity<List<EmpleadoDto>> listar(
            @RequestParam(name = "filtro", required = false) String filtro) {

        List<EmpleadoDto> empleados = empleadoService.listarEmpleados(filtro);

        log.info("Listado general completado. Total de empleados: {}", empleados.size());
        return ResponseEntity.ok(empleados);
    }

    // GET /ejecutivos/{uid}
    @GetMapping("/{uid}")
    public ResponseEntity<EmpleadoDto> obtener(@PathVariable String uid) throws Exception {
    	String safeUid = Validators.sanitizeUid(uid);
        EmpleadoDto empleado = empleadoService.loadUserByUsername(safeUid);

        log.info("Empleado {} obtenido con exito.", uid);
        return ResponseEntity.ok(empleado);
    }

    @PostMapping
    public ResponseEntity<Void> crear(@Valid @RequestBody EmpleadoDto dto,
            Principal principal,
            HttpServletRequest request) throws Exception {

        // Helpers locales
        final java.util.function.Predicate<String> notBlank = s -> s != null && !s.trim().isEmpty();
        final java.util.function.BiFunction<String, String, String> firstNonBlank = (a, b) -> notBlank.test(a) ? a
                : (b == null ? "" : b);

        // 1) Alta
        empleadoService.crearEmpleado(dto);

        // --- Actor ---
        String actor = principal != null ? HtmlUtils.htmlEscape(principal.getName()) : "system";


        // --- IP ---
        String ip = request.getHeader("X-Forwarded-For");
        if (notBlank.test(ip)) {
            int comma = ip.indexOf(',');
            ip = (comma > 0) ? ip.substring(0, comma).trim() : ip.trim();
        } else {
            ip = firstNonBlank.apply(request.getHeader("X-Real-IP"), request.getRemoteAddr());
        }

        // --- User-Agent ---
        String ua = request.getHeader("User-Agent");
        
         ip = HtmlUtils.htmlEscape(ip);
         ua = HtmlUtils.htmlEscape(ua);

        // 2) Auditoría
        auditoriaService.registrarAltaEmpleado(dto, actor, ip, ua);

        // 3) Envío de correo (con password en claro si pudimos descifrar; si no, con
        // hash o placeholder)
        final String email = safe(dto.getEmail());
        final String hashPassword = safe(dto.getPassword());

        if (notBlank.test(email)) {
            try {
                // Intentar descifrado si vinieron los campos del front
                String plainForMail = null;
                final String token = safe(dto.getToken());
                final String ivEnc = safe(dto.getIvEnc());
                final String planoEnc = safe(dto.getPlanoEnc());

                if (notBlank.test(hashPassword) && notBlank.test(token) && notBlank.test(ivEnc)
                        && notBlank.test(planoEnc)) {
                    try {
                        // cifrado/descifrado
                        plainForMail = cryptoFrontService
                                .decryptPlainFromFront(hashPassword, token, ivEnc, planoEnc)
                                .orElse(null);
                    } catch (Exception ce) {
                        // No detiene el flujo; solo registra
                        log.warn("No se pudo descifrar password para correo de {}: {}", email, ce.getMessage());
                    }
                }

                // Rol (null-safe)
                final String rolDesc = (safe(dto.getRol().getDescripcion()));

                // Regiones: junta descripciones o claves
                final String regionesStr = joinRegiones(dto);

                // Props para la plantilla
                Map<Propiedad, String> props = new HashMap<>();
                // Si logramos descifrar, va en claro; si no, envía el hash (o un placeholder si
                // prefieres)
                props.put(Propiedad.password, notBlank.test(plainForMail) ? plainForMail : hashPassword);
                props.put(Propiedad.empleado_email, email);
                props.put(Propiedad.empleado_nombre, safe(dto.getNombre()));
                props.put(Propiedad.empleado_rol, rolDesc);
                props.put(Propiedad.empleado_regiones, regionesStr);

                emailService.enviarCorreo(
                        email,
                        TipoEvento.alta_usuario,
                        props);

                log.info("Correo de alta enviado a {}", email);
            } catch (Exception e) {
                log.warn("No se pudo enviar correo de alta de usuario a uid={} email={}: {}",
                        dto.getUid(), email, e.getMessage());
            }
        } else {
            log.warn("No se envió correo: email vacío para uid={}", dto.getUid());
        }

        log.info("Empleado {} creado; auditoría registrada por {}", dto.getUid(), actor);
        return ResponseEntity.created(java.net.URI.create("/ejecutivos/" + dto.getUid())).build();
    }

    // ===== Helpers =====
    private String safe(String v) {
        return v == null ? "" : v.trim();
    }

    /** Une las regiones del DTO en una cadena "REG1, REG2, ..." */
    private String joinRegiones(EmpleadoDto dto) {
        try {
            if (dto == null || dto.getRegiones() == null)
                return "";
            return dto.getRegiones()
                    .stream()
                    .map(r -> {
                        try {
                            // usa descripción si hay; si no, cve; si no, vacío
                            String d = r.getDescripcion();
                            if (d != null && !d.trim().isEmpty())
                                return d.trim();
                            String c = r.getCve();
                            return c == null ? "" : c.trim();
                        } catch (Exception ignore) {
                            return "";
                        }
                    })
                    .filter(s -> s != null && !s.isEmpty())
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");
        } catch (Exception e) {
            return "";
        }
    }

    // PUT /ejecutivos/{uid}
    @PutMapping("/{uid}")
    public ResponseEntity<Void> actualizar(
            @PathVariable String uid,
            @RequestBody EmpleadoDto cambios,
            Principal principal,
            HttpServletRequest request) throws Exception {

    	 String safeUid = Validators.sanitizeUid(uid);
    	 
        // 1) Snapshot ANTES (desde LDAP)
        EmpleadoDto before = empleadoService.loadUserByUsername(safeUid); // ← necesitas este método en tu service

        // 2) Actualiza en LDAP
        empleadoService.actualizarEmpleado(safeUid, cambios);

        // 3) Snapshot DESPUÉS
        EmpleadoDto after = empleadoService.loadUserByUsername(safeUid);

        // Actor / red / agente
      
        String safeActor = HtmlUtils.htmlEscape(principal.getName() != null ? principal.getName() : "system");
        String ip = Validators.sanitizeHeader(request.getHeader("X-Forwarded-For"));
        if (ip == null || ip.isBlank())
            ip = Validators.sanitizeHeader(request.getRemoteAddr());
        String ua = Validators.sanitizeHeader(request.getHeader("User-Agent"));

        // 4) Registrar auditoría con before/after y comentario
        String nota = (cambios.getAuditNote() != null) ? cambios.getAuditNote().trim() : null;
        auditoriaService.registrarModificacionEmpleado(safeUid, before, after, safeActor, ip, ua, nota);

        log.info("Empleado {} actualizado y auditoria registrada por actor: {}", safeUid, safeActor);
        return ResponseEntity.noContent().build();
    }

    // DELETE /ejecutivos/{uid}
    @DeleteMapping("/{uid}")
    public ResponseEntity<Void> eliminar(
            @PathVariable String uid,
            Principal principal,
            HttpServletRequest request) throws Exception {

    	String safeUid = Validators.sanitizeUid(uid);
    	 
        // 1) Ejecuta eliminación
        empleadoService.eliminarEmpleado(safeUid);

        // 2) Datos de auditoría (igual que en tu PUT)
        String actor = principal != null ? principal.getName() : "system";
        String safeIp = Validators.sanitizeHeader(Optional.ofNullable(request.getHeader("X-Forwarded-For"))
                .filter(s -> !s.isBlank())
                .orElse(request.getRemoteAddr()));
        String safeUa = Validators.sanitizeHeader(request.getHeader("User-Agent"));

        // 3) Registrar en auditoría
        auditoriaService.registrarEliminacionUsuario(safeUid, actor, safeIp, safeUa);

        log.warn("Empleado {} eliminado y auditoria registrada por actor: {}", safeUid, actor);
        // 4) Respuesta
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{uid}/verificar-password")
    public ResponseEntity<Map<String, Object>> verificarPassword(
            @PathVariable String uid,
            @RequestParam(name = "modo", defaultValue = "bind") String modo,
            @RequestBody Map<String, String> body) throws Exception {
        String raw = body.get("password");
        String safeUid = Validators.sanitizeUid(uid);
        
        if (raw == null || raw.isBlank()) {
            log.warn("Fallo en verificación de password para UID {} - Password no proporcionado.", safeUid);
            return ResponseEntity.badRequest().body(Map.of(
                    "uid", safeUid,
                    "ok", false,
                    "error", "Falta 'password' en el body"));
        }

        boolean ok;
        try {
            ok = "hash".equalsIgnoreCase(modo)
                    ? empleadoService.verificarPasswordPorHash(safeUid, raw)
                    : empleadoService.verificarPasswordBind(safeUid, raw);
        } finally {
            java.util.Arrays.fill(raw.toCharArray(), '\0');
        }

        log.info("Verificacion de password para UID {} en modo {} resultó: {}", safeUid, modo, ok);

        return ResponseEntity.ok(Map.of(
                "uid", safeUid,
                "modo", modo,
                "ok", ok));
    }

    // PUT /ejecutivos/{uid}/activo
    @PutMapping("/{uid}/activo")
    public ResponseEntity<Void> actualizarActivo(
            @PathVariable String uid,
            @RequestBody Map<String, Object> body,
            Principal principal,
            HttpServletRequest request) {

        boolean activo = Boolean.parseBoolean(String.valueOf(body.get("activo")));

        empleadoService.actualizarEstadoActivo(uid, activo);

        String actor = principal != null ? principal.getName() : "system";
        String ua = Validators.sanitizeHeader(request.getHeader("User-Agent"));
        String ip = Optional.ofNullable(request.getHeader("X-Forwarded-For"))
                            .filter(s -> !s.isBlank())
                            .map(Validators::sanitizeHeader)
                            .orElse(Validators.sanitizeHeader(request.getRemoteAddr()));

        auditoriaService.registrarCambioActivo(uid, activo, actor, ip, ua);

        log.info("Estado activo de UID {} cambiado a {} y auditoria registrada.", uid, activo);
        return ResponseEntity.noContent().build();
    }

    // POST /ejecutivos/reportes/usuarios-internos
    @PostMapping(value = "/reportes/usuarios-internos", consumes = "application/json", produces = "application/json")
    public ResponseEntity<List<EmpleadoReporteDto>> listarUsuariosInternos(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate desde,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate hasta,
            @AuthenticationPrincipal UserDetails u,
            @RequestBody(required = false) FiltroUsuariosInternosDto filtro,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "15") int tamanio,
            @RequestParam(defaultValue = "uid") String ordenarPor,
            HttpServletRequest request) {

    	String actor = "system";
        if (u != null && u.getUsername() != null) {
            actor = Validators.sanitizarUid(u.getUsername());
        }
        
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank())
            ip = request.getRemoteAddr();
        ip = Validators.sanitizeHeader(ip);
        
        String ua = Validators.sanitizeHeader(request.getHeader("User-Agent"));

        tamanio = Validators.tamanioRazonable(tamanio);
        pagina = Validators.paginadoRazonable(pagina);
        
        Validators.validaFechaDesde(desde);
        Validators.validaFechaHasta(hasta);
       

        if (filtro == null)
            filtro = new FiltroUsuariosInternosDto();

        List<EmpleadoReporteDto> resultado = empleadoService.listarUsuariosInternos(
                desde, hasta, actor, filtro, pagina, tamanio, ordenarPor);

        try {
            auditoriaService.registrarConsultaListadoUsuarios(
                    actor, ip, ua, filtro, desde, hasta,
                    filtro.getCampoFecha() != null ? filtro.getCampoFecha()
                            : FiltroUsuariosInternosDto.CampoFecha.FECHA_ALTA,
                    resultado.size());
        } catch (Exception ignore) {
            log.warn("Fallo al registrar auditoria de consulta de listado de usuarios.");
        }

        log.info("Reporte de usuarios internos completado. Resultados: {}", resultado.size());
        return ResponseEntity.ok(resultado);
    }

    @GetMapping("/{correo}/eliminacion/guard")
    public ResponseEntity<DeletionGuardResponse> guardEliminar(@PathVariable("correo") String correo) {
        log.info("Guard eliminar para: {}", correo);
        DeletionGuardResponse resp = guardService.evaluate(correo);
        return ResponseEntity.ok(resp);
    }

    /**
     * GET /asignaciones/folios?email=j.becerra@bim.mx
     * Devuelve folios asignados en estatus PR/PE:
     * - monetarias (no programadas)
     * - programadas
     * - juridicas
     */
    @GetMapping("/folios")
    public ResponseEntity<FoliosAsignadosResponse> folios(@RequestParam("email") String email) {
        return ResponseEntity.ok(service.listarFolios(email));
    }
}