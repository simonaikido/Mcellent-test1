package com.bim.seif.controllers;

import com.bim.seif.models.OperacionJuridica;
import com.bim.seif.models.dto.*;
import com.bim.seif.models.wrappers.OperacionesJuridicasWrapper;
import com.bim.seif.services.OperacionJuridicaService;
import com.bim.seif.services.OperacionService;
import com.bim.seif.utils.JSONUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import javax.validation.Valid;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.bim.seif.utils.Validators;

@Slf4j
@RestController
@RequestMapping("/operaciones")
@RequiredArgsConstructor
public class OperacionController {

    private final OperacionService operacionService;
    private final OperacionJuridicaService operacionJuridicaService;

    @GetMapping("{folioInstruccion}/monetarias")
    public ResponseEntity<List<OperacionMonetariaDto>> obtenerOperacionesMonetarias(
            @PathVariable String folioInstruccion) {
        List<OperacionMonetariaDto> operaciones = operacionService.obtenerOperacionesMonetarias(folioInstruccion);
        return ResponseEntity.ok(operaciones);
    }

    @PutMapping("{idOperacion}") // Nuevo endpoint para actualizaci
    public ResponseEntity<Void> actualizarOperacion(
            @PathVariable Long idOperacion, @RequestBody RevisionComprobanteDto revision) {
        operacionService
                .actualizarOperacion(idOperacion, revision);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/monetarias/tipos")
    public ResponseEntity<List<TipoOperacionMonetariaDto>> obtenerTiposOperacion() {
        List<TipoOperacionMonetariaDto> tipos = operacionService.obtenerTiposOperacionMonetaria();
        return ResponseEntity.ok(tipos);
    }

    @GetMapping("/monetarias/tipos/{tipoOperacion}/configuraciones")
    public ResponseEntity<List<ConfiguracionOperacionMonetariaDto>> obtenerConfiguracionMonetaria(
            @PathVariable String tipoOperacion) {
        List<ConfiguracionOperacionMonetariaDto> configuraciones = operacionService
                .obtenerConfiguracionMonetaria(tipoOperacion);
        return ResponseEntity.ok(configuraciones);
    }

    @PostMapping("{folioInstruccion}/monetarias")
    public ResponseEntity<List<OperacionMonetariaDto>> guardarOperacionesMonetarias(
            @PathVariable String folioInstruccion, @RequestBody List<OperacionMonetariaRequestDto> operaciones)
            throws InterruptedException {
        List<OperacionMonetariaDto> operacionesPersistidas = operacionService
                .guardarOperacionesMonetarias(folioInstruccion, operaciones);
        return ResponseEntity.ok(operacionesPersistidas);
    }

    @PostMapping("/monetarias/{idOperacion}/revisiones")
    // CHECKMARX-FALSE-POSITIVE: El DTO recibido se mapea de forma segura.
    // Motivo: Patrón estándar de Spring Boot, sin inyección ni manipulación de consultas.
    public ResponseEntity<RevisionOperacionMonetariaDto> guardarRevisionesMonetarias(
            // el username/correo viene del sub del JWT
            @AuthenticationPrincipal(expression = "claims['sub']") String validador,
            @PathVariable Long idOperacion,
            @RequestBody RevisionOperacionMonetariaDto revision) {

        // obtiene la autoridad principal desde el JWT ya convertido a authorities
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String authority = (auth != null && auth.getAuthorities() != null && !auth.getAuthorities().isEmpty())
                ? auth.getAuthorities().iterator().next().getAuthority()
                : null;

        if (authority == null || validador == null || validador.isBlank()) {
            // sin identidad o sin rol → 403
            return ResponseEntity.status(403).build();
        }

        RevisionOperacionMonetariaDto result = operacionService.guardarRevisionesMonetarias(validador, authority,
                idOperacion, revision);

        return ResponseEntity.ok(result);
    }

    @PostMapping("/monetarias/reportes")
    public ResponseEntity<List<OperacionMonetariaDto>> listarOperacionesMonetarias(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate desde,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate hasta,
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody OperacionMonetariaDto filtro,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "15") int tamanio,
            @RequestParam(defaultValue = "id") String ordenarPor) {

        // Obtener el correo o username del claim "sub"
        String username = jwt.getSubject();
        
        
        Validators.sanitizeUsername(username);

        // Validaciones básicas
        if (desde != null && hasta != null && hasta.isBefore(desde)) {
            return ResponseEntity.badRequest().build();
        }

        // Llamada al servicio con el usuario obtenido desde el token
        List<OperacionMonetariaDto> instruccionesMonetarias = operacionService.obtenerOperacionesMonetarias(
                desde,
                hasta,
                username,
                filtro,
                pagina,
                tamanio,
                ordenarPor);

        return ResponseEntity.ok(instruccionesMonetarias);
    }

    @PutMapping("{folioInstruccion}/monetarias") // Nuevo endpoint para actualizaci
    public ResponseEntity<List<OperacionMonetariaDto>> actualizarOperacionesMonetarias(
            @PathVariable String folioInstruccion, @RequestBody List<OperacionMonetariaRequestDto> operaciones) {
        List<OperacionMonetariaDto> operacionesActualizadas = operacionService
                .actualizarOperacionesMonetarias(folioInstruccion, operaciones);
        return ResponseEntity.ok(operacionesActualizadas);
    }

    @PutMapping("/{operacionId}/monetarias/revisiones/omitir")
    public ResponseEntity<List<RevisionOperacionMonetariaDto>> omitirRevisionesOperacionMonetaria(
            @PathVariable Long operacionId,
            @RequestBody(required = false) String comentarios // Opcional: para agregar un comentario al omitir
    ) {
        List<RevisionOperacionMonetariaDto> revisionesOmitidas = operacionService
                .omitirRevisionesOperacionMonetaria(operacionId, comentarios);
        return ResponseEntity.ok(revisionesOmitidas);
    }

    @DeleteMapping("/monetarias/revisiones/eliminar")
    public ResponseEntity<Void> eliminarRevisionesMonetarias(@RequestBody List<Long> operacionMonetariaIds) {
        operacionService.eliminarRevisionesPorOperacionMonetariaIds(operacionMonetariaIds);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT); // 204 No Content para eliminación exitosa
    }

    // No monetarias

    @GetMapping("{folioInstruccion}/juridicas")
    public ResponseEntity<List<OperacionJuridicaDto>> obtenerOperacionesJuridicas(
            @PathVariable String folioInstruccion) {
        List<OperacionJuridicaDto> operaciones = operacionService.obtenerOperacionesJuridicas(folioInstruccion);
        return ResponseEntity.ok(operaciones);
    }

    @GetMapping("/juridicas/tipos")
    public ResponseEntity<List<TipoOperacionJuridicaDto>> obtenerTiposOperacionJuridica() {
        List<TipoOperacionJuridicaDto> tipos = operacionService.obtenerTiposOperacionJuridica();
        return ResponseEntity.ok(tipos);
    }

    @GetMapping("/juridicas/formatosbim")
    public ResponseEntity<List<FormatoBimDto>> obtenerFormatosBim() {
        List<FormatoBimDto> formatoBimDto = operacionService.obtenerListaFormatosBim();
        return ResponseEntity.ok(formatoBimDto);
    }

    @GetMapping("/juridicas/archvios")
    public ResponseEntity<List<ListaArchivosJuridicaDto>> obtenerarchivosBim() {
        List<ListaArchivosJuridicaDto> ListaArchivosDto = operacionService.obtenerListaArchivos();
        return ResponseEntity.ok(ListaArchivosDto);
    }

    @GetMapping("/juridicas/archivosSugeridos/{folioInstruccion}")
    public ResponseEntity<String> obtenerArchivosSugeridos(@PathVariable String folioInstruccion) {
        String operaciones = operacionService.obtenerArchivosSugeridos(folioInstruccion);

        return ResponseEntity
                .ok(JSONUtils.covertObjectToJSON(Collections.singletonMap("archivosSugeridos", operaciones)));
    }

    @PostMapping("/juridicas/solicitud/{folioInstruccion}")
    public ResponseEntity<String> guardarOperacionesJuridicas_SolicitudArchvos(
            @AuthenticationPrincipal UserDetails validador,
            @PathVariable String folioInstruccion,
            @Valid @RequestBody OperacionesJuridicasWrapper request) {

        try {
        	 List<OperacionJuridicaRequestDto> operaciones = request.getOperaciones();
        	 List<SolicitudArchivoJuridicaRequestDto> solicitudArchivos = request.getSolicitudDeArchivos();

            if (operaciones == null || operaciones.isEmpty()) {
                return ResponseEntity.badRequest().body("No se recibieron operaciones");
            }

            for (OperacionJuridicaRequestDto op : operaciones) {
                if (op.getId() == null) {
                    return ResponseEntity.badRequest().body("Cada operación debe traer su id");
                }

                operacionJuridicaService.guardarOperacionJuridica_SolicitudArchivos(
                        op, solicitudArchivos, op.getId(), folioInstruccion);
            }

            return ResponseEntity.ok(
                    JSONUtils.covertObjectToJSON(
                            Collections.singletonMap("Exito", "Operaciones enviadas al cliente")));

        } catch (org.hibernate.TransientPropertyValueException e) {
            log.error("Relación transiente al guardar operación jurídica: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Relación no persistida: " + e.getPropertyName());
        } catch (Exception e) {
            log.error("Error al guardar operaciones jurídicas ({}): {}", e.getClass().getSimpleName(),
                    e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error al guardar operaciones jurídicas");
        }
    }

    // Sin solicitud de archivos a aprovacion
    @PostMapping("/juridicas/{folioInstruccion}")
    public ResponseEntity<String> guardarOperacionesJuridicasParaAprobar(@AuthenticationPrincipal UserDetails validador,
            @PathVariable String folioInstruccion,
            @RequestBody Map<String, Object> requestBody) {

        try {
            List<Object> operacionJuridicas = (List<Object>) requestBody.get("operaciones");

            for (Object operacionJuridica : operacionJuridicas) {
                LinkedHashMap oj = (LinkedHashMap) operacionJuridica;

                OperacionJuridica operacion = operacionJuridicaService.guardarOperacionJuridica(oj, folioInstruccion);
            }
            // emailService.sendEmail("cliente@ejemplo.com", "Instrucción Rechazada",
            // mensajeCorreo); // Reemplazar con el correo del cliente
            return ResponseEntity.ok(JSONUtils
                    .covertObjectToJSON(Collections.singletonMap("Exito", "Operaciones enviadas a aprobacion")));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    @GetMapping("/revision")
    public ResponseEntity<List<InstruccionJuridicaDto>> getInstruccionesParaRevision(
            @RequestHeader("X-User-Rol") String rol) {
        List<InstruccionJuridicaDto> instrucciones = operacionService.getInstruccionesParaRevision(rol);
        return ResponseEntity.ok(instrucciones);
    }

    @GetMapping("/aprobacion")
    public ResponseEntity<List<InstruccionJuridicaDto>> getInstruccionesParaAprobacion(
            @RequestHeader("X-User-Rol") String rol) {
        List<InstruccionJuridicaDto> instrucciones = operacionService.getInstruccionesParaAprobacion(rol);
        return ResponseEntity.ok(instrucciones);
    }

    @GetMapping("/pendientes")
    public ResponseEntity<List<InstruccionJuridicaDto>> getInstruccionesPendientes(
            @RequestHeader("X-User-Rol") String rol) {
        List<InstruccionJuridicaDto> instrucciones = operacionService.getInstruccionesPendientes(rol);
        return ResponseEntity.ok(instrucciones);
    }

    @GetMapping("/clasificadas/juridicas/{folio}")
    public ResponseEntity<String> obtenerOperaciones_ArchivosSugeridos(@PathVariable String folioInstruccion) {
        List<OperacionJuridicaDto> operaciones = operacionService.obtenerOperacionesJuridicas(folioInstruccion);

        return ResponseEntity
                .ok(JSONUtils.covertObjectToJSON(Collections.singletonMap("archivosSugeridos", operaciones)));
    }

    @GetMapping("/juridicas/archivossolicitadosporoperacion/{idOperacion}")
    public ResponseEntity<List<SolicitudArchivoJuridicaDto>> obtenerArchivosSolicitadosPorOperacion(
            @PathVariable String idOperacion) {
        List<SolicitudArchivoJuridicaDto> archviosSugeridos = operacionJuridicaService
                .obtenerArchivosSolicitadosPorOperacion(Long.valueOf(idOperacion));
        return ResponseEntity.ok(archviosSugeridos);
    }

    @GetMapping("/Juridicas/Validar")
    public List<InstruccionJuridicaDto> getInstruccionesPorRol(
            @AuthenticationPrincipal(expression = "claims['sub']") String empleado) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String rol = null;

        if (auth != null && auth.getAuthorities() != null && !auth.getAuthorities().isEmpty()) {
            rol = auth.getAuthorities().iterator().next().getAuthority();
        }

        System.out.println("Empleado: " + empleado + " | ROL: " + rol);

        return operacionJuridicaService.getInstruccionesJuridicasPorRol(rol);
    }

    // Nuevo endpoint para guardar la revisión de una operación jurídica
    @PostMapping("/juridicas/{idOperacion}/validaciones")
    public ResponseEntity<RevisionOperacionJuridicaDto> guardarRevisionJuridica(
            @AuthenticationPrincipal Jwt validador,
            @PathVariable Long idOperacion,
            @RequestBody RevisionOperacionJuridicaDto revisionDto) {

        // Se llama al servicio para guardar la revisión y actualizar el estatus de la
        // operación jurídica
        RevisionOperacionJuridicaDto revisionGuardada = operacionJuridicaService.guardarRevisionOperacionJuridica(
                idOperacion,
                revisionDto,
                validador.getSubject());
        return ResponseEntity.ok(revisionGuardada);
    }

    // Endpoint para revisar una operación
    @PostMapping("/juridicas/{idOperacion}/revisar")
    public ResponseEntity<RevisionOperacionJuridicaDto> revisarOperacionJuridica(
            @AuthenticationPrincipal Jwt validador,
            @PathVariable Long idOperacion,
            @RequestBody RevisionOperacionJuridicaDto revisionDto) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        RevisionOperacionJuridicaDto revisionGuardada = operacionJuridicaService.revisarOperacionJuridica(
                idOperacion,
                revisionDto,
                validador.getSubject());
        return ResponseEntity.ok(revisionGuardada);
    }

    @PostMapping("/juridicas/reportes")
    public ResponseEntity<List<OperacionJuridicaDto>> listarOperacionesJuridicas(
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate desde,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate hasta,
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody OperacionJuridicaDto filtro,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "15") int tamanio,
            @RequestParam(defaultValue = "id") String ordenarPor) {

        // obtener el usuario autenticado desde el token
        String username = jwt.getSubject();
        
        Validators.sanitizeUsername(username);

        // Validación opcional de fechas
        if (desde != null && hasta != null && hasta.isBefore(desde)) {
            return ResponseEntity.badRequest().body(null);
        }

        // Llamada al servicio
        List<OperacionJuridicaDto> operaciones = operacionService.obtenerOperacionesJuridicas(
                desde,
                hasta,
                username,
                filtro,
                pagina,
                tamanio,
                ordenarPor);

        return ResponseEntity.ok(operaciones);
    }

    // Endpoint para aprobar una operación
    @PostMapping("/juridicas/{idOperacion}/aprobar")
    public ResponseEntity<RevisionOperacionJuridicaDto> aprobarOperacionJuridica(
            @AuthenticationPrincipal Jwt validador,
            @PathVariable Long idOperacion,
            @RequestBody RevisionOperacionJuridicaDto revisionDto) {

        RevisionOperacionJuridicaDto revisionGuardada = operacionJuridicaService.aprobarOperacionJuridica(
                idOperacion,
                revisionDto,
                validador.getSubject());
        return ResponseEntity.ok(revisionGuardada);
    }

    @GetMapping("/juridicas/{idOperacion}/historial")
    public List<RevisionOperacionJuridicaDto> getHistorialOperacion(
            @PathVariable("idOperacion") long operacionJuridicaId) {
        return operacionJuridicaService.getHistorialByOperacionJuridicaId(operacionJuridicaId);
    }

    // programadas
    @PostMapping("/programadas/monetarias")
    public ResponseEntity<List<OperacionMonetariaDto>> guardarProgramadas(
            @RequestBody List<OperacionMonetariaRequestDto> operaciones) throws InterruptedException {
        return ResponseEntity.ok(operacionService.guardarOperacionesMonetarias("programadas", operaciones));
    }

    @PostMapping("/juridicas/{idOperacion}/solicitar-correccion")
    public ResponseEntity<RevisionOperacionJuridicaDto> solicitarCorreccionOperacionJuridica(
            @AuthenticationPrincipal Jwt validador,
            @PathVariable Long idOperacion,
            @RequestBody RevisionOperacionJuridicaDto revisionDto) {

        RevisionOperacionJuridicaDto revisionGuardada = operacionJuridicaService.solicitarCorreccionOperacionJuridica(
                idOperacion,
                revisionDto,
                validador.getSubject());
        return ResponseEntity.ok(revisionGuardada);
    }

    @PostMapping("/juridicas/{idOperacion}/rechazar")
    public ResponseEntity<RevisionOperacionJuridicaDto> rechazarOperacionJuridica(
            @AuthenticationPrincipal Jwt validador,
            @PathVariable Long idOperacion,
            @RequestBody RevisionOperacionJuridicaDto revisionDto) {

        RevisionOperacionJuridicaDto revisionGuardada = operacionJuridicaService.rechazarOperacionJuridica(
                idOperacion,
                revisionDto,
                validador.getSubject());
        return ResponseEntity.ok(revisionGuardada);
    }

    @PutMapping("/juridicas/{idOperacion}/envio-notaria")
    public ResponseEntity<Void> actualizarEnvioNotaria(@PathVariable Long idOperacion) {
        operacionJuridicaService.actualizarEnvioNotaria(idOperacion);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/juridicas/{id}")
    // CHECKMARX-FALSE-POSITIVE: El objeto DTO se valida y se usa de forma segura antes de persistirlo.
    // Motivo: Patrón estándar de Spring Boot para intercambio de datos (DTO). No hay inyección SQL ni concatenaciones inseguras.
    public ResponseEntity<Void> actualizarOperacionJuridica(@PathVariable long id,
            @RequestBody OperacionJuridicaDto dto) {
        operacionService.finalizarOperacionJuridica(id, dto);
        return ResponseEntity.ok().build();
    }

}
