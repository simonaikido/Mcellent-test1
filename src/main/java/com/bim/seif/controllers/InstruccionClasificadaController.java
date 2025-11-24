package com.bim.seif.controllers;

import com.bim.seif.models.dto.InstruccionDto;
import com.bim.seif.models.dto.InstruccionJuridicaDto;
import com.bim.seif.models.dto.InstruccionMonetariaDto;
import com.bim.seif.models.dto.OperacionMonetariaDto;
import com.bim.seif.models.*;
import com.bim.seif.services.*;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.naming.InvalidNameException;

@Slf4j
@RestController
@RequestMapping("/instrucciones")
public class InstruccionClasificadaController {

    @Autowired
    private InstruccionClasificadaService instruccionClasificadaService;

    @Autowired
    private CustomerService clienteService;

    @Autowired
    private InstruccionService instruccionService;

    @Autowired
    private ValidacionSerice validacionSerice;

    @GetMapping
    public ResponseEntity<List<InstruccionDto>> listarInstruccionesSinClasificar(
            @AuthenticationPrincipal Jwt jwt) throws UsernameNotFoundException, InvalidNameException {

        String username = jwt.getSubject();
        List<InstruccionDto> instrucciones = instruccionService.obtenerInstruccionesSinClasificar(username);
        return ResponseEntity.ok(instrucciones);
    }

    @PostMapping("/{id}")
    public ResponseEntity<Void> clasificarInstruccion(@PathVariable String id,
            @RequestParam(name = "rmonetaria", required = false) String responsableMonetaria,
            @RequestParam(name = "rjuridica", required = false) String responsableJuridica) {
        instruccionService.calsificarInstruccion(id, responsableMonetaria, responsableJuridica);
        return ResponseEntity.ok().build();
    }

    // --- helpers ---
    private boolean visibleOperacionPorProgramacion(InstruccionMonetariaDto instruccion,
            OperacionMonetariaDto op,
            LocalDate hoyMx) {
        // Si la instrucción NO es programada → la operación siempre es visible (por
        // programación)
        if (!instruccion.isProgramada())
            return true;

        // Si es programada, la operación debe tener fechaRegistro y coincidir con HOY
        if (op.getFechaRegistro() == null)
            return false;
        LocalDate f = op.getFechaRegistro().toLocalDate();
        // Si quieres incluir vencidas:
        return !f.isAfter(hoyMx);
    }

    @GetMapping("/monetarias")
    public ResponseEntity<List<InstruccionMonetariaDto>> obtenerInstruccionesMonetariasPorValidador(
            @AuthenticationPrincipal(expression = "claims['sub']") String validador,
            @RequestParam(name = "isMesaControl", required = false, defaultValue = "false") boolean isMesaControl) {

        // Authorities mapeadas desde el JWT (claim "p" -> ROLE_*)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Set<String> roles = new HashSet<>();
        if (auth != null && auth.getAuthorities() != null) {
            for (GrantedAuthority ga : auth.getAuthorities()) {
                if (ga != null && ga.getAuthority() != null) {
                    roles.add(ga.getAuthority().toUpperCase());
                }
            }
        }

        String authority = roles.isEmpty() ? "" : roles.iterator().next();
        LocalDate hoyMx = LocalDate.now(ZoneId.of("America/Mexico_City"));

        List<InstruccionMonetariaDto> instrucciones = new ArrayList<>();

        // ROLE_CI ve TODAS las instrucciones (no filtramos instrucciones, solo
        // operaciones)
        if ("ROLE_CI".equalsIgnoreCase(authority) || roles.contains("ROLE_CI")) {
            instrucciones = instruccionService.obtenerTodasInstruccionesMonetarias();
            log.info("ROLE_CI - Total instrucciones: {}", instrucciones.size());

            for (InstruccionMonetariaDto i : instrucciones) {
                int opsAntes = (i.getOperaciones() == null) ? 0 : i.getOperaciones().size();

                if (i.getOperaciones() != null) {
                    // 1) Filtro por programación/fecha (a nivel operación)
                    List<OperacionMonetariaDto> opsProg = i.getOperaciones().stream()
                            .filter(op -> {
                                boolean vis = visibleOperacionPorProgramacion(i, op, hoyMx);
                                if (i.isProgramada()) {
                                    log.debug("[{}] opId={} fechaOp={} visiblePorProgramacion={}",
                                            i.getFolio(),
                                            op.getId(),
                                            op.getFechaRegistro(),
                                            vis);
                                }
                                return vis;
                            })
                            .collect(Collectors.toList());

                    // 2) No hay más filtros para CI; asignamos
                    i.setOperaciones(opsProg);
                }

                int opsDespues = (i.getOperaciones() == null) ? 0 : i.getOperaciones().size();
                log.debug("Instrucción {} programada={} → ops antes:{} | después(programación):{}",
                        i.getFolio(), i.isProgramada(), opsAntes, opsDespues);
            }

            // IMPORTANTE: NO quitamos instrucciones aunque queden sin operaciones
            return ResponseEntity.ok(instrucciones);
        }

        // ROLE_MC (Mesa de Control)
        if ("ROLE_MC".equalsIgnoreCase(authority) || roles.contains("ROLE_MC")) {
            instrucciones = instruccionService.obtenerInstruccionesMonetariasMesaControl();
            log.info("ROLE_MC - Total instrucciones: {}", instrucciones.size());

            for (InstruccionMonetariaDto i : instrucciones) {
                int opsAntes = (i.getOperaciones() == null) ? 0 : i.getOperaciones().size();

                if (i.getOperaciones() != null) {
                    // 1) Filtro por programación/fecha
                    List<OperacionMonetariaDto> ops = i.getOperaciones().stream()
                            .filter(op -> visibleOperacionPorProgramacion(i, op, hoyMx))
                            .collect(Collectors.toList());
                    int opsProg = ops.size();

                    // 2) Filtro por rol de mesa de control
                    ops = ops.stream()
                            .filter(o -> o.getTipoOperacion().isValidacionMesaControl())
                            .collect(Collectors.toList());
                    int opsMC = ops.size();

                    i.setOperaciones(ops);
                    log.debug("Instrucción {} → ops antes:{} | después(programación):{} | después(MC):{}",
                            i.getFolio(), opsAntes, opsProg, opsMC);
                }
            }

            // NO quitamos instrucciones aunque queden sin operaciones
            return ResponseEntity.ok(instrucciones);
        }

        // Otros roles (validación general/montos)
        instrucciones = instruccionService.obtenerInstruccionesMonetariasPorValidador(validador);
        log.info("Otros roles ({}) - Total instrucciones: {}", authority, instrucciones.size());

        List<String> rolesVBGenerales = Arrays.asList(Rol.ROLE_GA.name(), Rol.ROLE_GL.name());
        boolean esVBGeneral = roles.stream().anyMatch(rolesVBGenerales::contains);
        log.info("esVBGeneral = {}", esVBGeneral);

        for (InstruccionMonetariaDto i : instrucciones) {
            int opsAntes = (i.getOperaciones() == null) ? 0 : i.getOperaciones().size();

            if (i.getOperaciones() == null)
                continue;

            // 1) Filtro por programación/fecha
            List<OperacionMonetariaDto> ops = i.getOperaciones().stream()
                    .filter(op -> visibleOperacionPorProgramacion(i, op, hoyMx))
                    .collect(Collectors.toList());
            int opsProg = ops.size();

            // 2) Filtro por rol: general o por montos
            ops = ops.stream().filter(o -> {
                if (esVBGeneral) {
                    return o.getTipoOperacion().isValidacionGeneral();
                } else {
                    String authToUse = authority;
                    if (authToUse == null || authToUse.isBlank())
                        return false;

                    Long montoValidar = validacionSerice.obtenerNivelesAprobacion()
                            .get(Rol.valueOf(authToUse));
                    montoValidar = (montoValidar == null) ? 0L : montoValidar;

                    long tc = (o.getMonto().getTipoCambio() == 0)
                            ? o.getMonto().getDivisa().getTipoCambioMonedaNacional()
                            : Long.parseLong(String.valueOf(o.getMonto().getTipoCambio()));

                    long montoMx = o.getMonto().getMonto() * tc;

                    return o.getTipoOperacion().isValidacionMontos() && (montoMx >= montoValidar);
                }
            }).collect(Collectors.toList());
            int opsRol = ops.size();

            i.setOperaciones(ops);
            log.debug("Instrucción {} programada={} → ops antes:{} | después(programación):{} | después(rol):{}",
                    i.getFolio(), i.isProgramada(), opsAntes, opsProg, opsRol);
        }

        // NO quitamos instrucciones aunque queden sin operaciones
        log.info("Total instrucciones retornadas (sin filtrar lista): {}", instrucciones.size());
        return ResponseEntity.ok(instrucciones);
    }

    @GetMapping("/clasificadas")
    public ResponseEntity<List<InstruccionMonetariaDto>> listarInstruccionesEnProceso(

            @AuthenticationPrincipal(expression = "claims['sub']") String empleado) { // <- mismo nombre, ahora String
                                                                                      // (email)
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isEA = auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_EA".equalsIgnoreCase(a.getAuthority()));

        if (!isEA) {
            return ResponseEntity.status(403).build();
        }

        // 'empleado' ya es el correo/username del token (sub)
        List<InstruccionMonetariaDto> instruccionesMonetarias = instruccionService
                .obtenerInstruccionesMonetarias(empleado);

        return ResponseEntity.ok(instruccionesMonetarias);
    }

    @PreAuthorize("hasRole('EL')")
    @GetMapping("/clasificadas/juridicas")
    public ResponseEntity<List<InstruccionJuridicaDto>> listarInstruccionesJuridicasEnProceso(
            @AuthenticationPrincipal(expression = "claims['sub']") String empleado) throws InterruptedException {

        return ResponseEntity.ok(instruccionService.obtenerInstruccionesJuridicas(empleado));
    }

    @GetMapping("/clasificadas/juridicas/{folio}")
    public ResponseEntity<InstruccionJuridicaDto> obtenerInstruccionesJuridicaPorFolio(@PathVariable String folio) {
        InstruccionJuridicaDto instruccionJuridica = instruccionService.obtenerInstruccionJuridicaPorFolio(folio);
        Cliente cliente = clienteService.obtenerClientePorEmail(instruccionJuridica.getInstruccion().getClienteCarga());
        String nombre = cliente.getNombre() + " " + cliente.getApellidoPaterno() + " " + cliente.getApellidoMaterno();
        instruccionJuridica.setNombreClienteInstructor(nombre);

        return ResponseEntity.ok(instruccionJuridica);
    }

    @PostMapping("/juridicas/reportes")
    public ResponseEntity<List<InstruccionJuridicaDto>> listarInstruccionesJuridicas(
            @AuthenticationPrincipal UserDetails empleado,
            @RequestBody InstruccionJuridicaDto filtro,
            @RequestParam(defaultValue = "0") int pagina,
            @RequestParam(defaultValue = "15") int tamanio,
            @RequestParam(defaultValue = "folio") String ordenarPor) {

        List<InstruccionJuridicaDto> instruccionesMonetarias = instruccionService.obtenerInstruccionesJuridicas(filtro,
                pagina, tamanio, ordenarPor);
        return ResponseEntity.ok(instruccionesMonetarias);
    }

    @GetMapping("/monetarias/consulta")
    public ResponseEntity<List<InstruccionMonetariaDto>> listarInstruccionesMonetariasConOtrosEstatus(
            @AuthenticationPrincipal(expression = "claims['sub']") String empleado) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        Set<String> roles = new HashSet<>();
        if (auth != null && auth.getAuthorities() != null) {
            for (GrantedAuthority ga : auth.getAuthorities()) {
                if (ga != null && ga.getAuthority() != null) {
                    roles.add(ga.getAuthority().toUpperCase());
                }
            }
        }

        boolean hasEaRole = roles.contains("ROLE_EA");
        boolean hasElRole = roles.contains("ROLE_EL");

        List<InstruccionMonetariaDto> instruccionesMonetarias = new ArrayList<>();

        if (hasEaRole) {
            instruccionesMonetarias = instruccionService.obtenerInstruccionesMonetariasConOtrosEstatus(empleado);
        } else if (hasElRole) {
            // Si tienes un método específico para legales, úsalo aquí.
            // instruccionesMonetarias =
            // instruccionService.obtenerInstruccionesLegalesConOtrosEstatus(empleado);
            instruccionesMonetarias = instruccionService.obtenerInstrucciones(empleado);
        } else {
            instruccionesMonetarias = instruccionService.obtenerInstrucciones(empleado);
        }

        return ResponseEntity.ok(instruccionesMonetarias);
    }

    @GetMapping("/{id}")
    public ResponseEntity<InstruccionClasificada> obtenerInstruccionPorId(@PathVariable Long id) {

        InstruccionClasificada instruccion = instruccionClasificadaService.obtenerInstruccionPorId(id);
        if (instruccion != null) {
            return ResponseEntity.ok(instruccion);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/filtrar")
    public ResponseEntity<List<InstruccionClasificada>> filtrarInstrucciones(
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String empleado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin) {

        List<InstruccionClasificada> instrucciones = instruccionClasificadaService.filtrarInstrucciones(tipo, region,
                empleado, fechaInicio, fechaFin);
        return ResponseEntity.ok(instrucciones);
    }

    @GetMapping("/buscar")
    public ResponseEntity<List<InstruccionClasificada>> buscarInstruccionesPorFideicomiso(
            @RequestParam String fideicomiso) {

        List<InstruccionClasificada> instrucciones = instruccionClasificadaService
                .buscarInstruccionesPorFideicomiso(fideicomiso);
        return ResponseEntity.ok(instrucciones);
    }

    @GetMapping("/ordenar")
    public ResponseEntity<List<InstruccionClasificada>> listarInstruccionesOrdenadasPorFecha() {

        List<InstruccionClasificada> instrucciones = instruccionClasificadaService
                .listarInstruccionesOrdenadasPorFecha();
        return ResponseEntity.ok(instrucciones);
    }

    @PutMapping("/estatus/{folio}")
    public ResponseEntity<Void> actualizarEstatusInstruccionMonetaria(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String folio,
            @RequestBody Map<String, String> requestBody) {

        if (jwt == null) {
            return ResponseEntity.status(401).build(); // No autenticado
        }

        String nuevoEstatus = requestBody.get("estatus");
        if (nuevoEstatus == null || nuevoEstatus.isEmpty()) {
            return ResponseEntity.badRequest().build(); // Retorna 400 si el estatus no se proporciona
        }

        instruccionService.actualizarEstatusInstruccionMonetaria(folio, nuevoEstatus);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/rechazar")
    public ResponseEntity<Void> rechazarInstruccion(@PathVariable Long id, @RequestBody String comentarioRechazo) {

        instruccionClasificadaService.procesarRechazo(id, comentarioRechazo);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/comprobantes")
    @Secured({ "ROLE_MC" })
    public ResponseEntity<List<InstruccionMonetariaDto>> obtenerInstruccionesPorValidar() {
        List<InstruccionMonetariaDto> instrucciones = new ArrayList<>();
        instrucciones = instruccionService.obtenerInstruccionesConComprobates();

        return ResponseEntity.ok(instrucciones);
    }

    // Nuevo endpoint para actualizar el estatus de la InstruccionJuridicas
    @PutMapping("/juridicas/{folio}")
    public ResponseEntity<Void> actualizarEstatusInstruccionJuridica(@PathVariable String folio,
            @RequestParam EstatusInstruccionEnum estatus) {
        instruccionService.actualizarEstatusInstruccionJuridica(folio, estatus);
        return ResponseEntity.ok().build();
    }

    /**
     * Endpoint para reclasificar una Instruccion monetaria.
     * Elimina la asociación monetaria, permitiendo que la instrucción reaparezca en
     * "sin clasificar"
     * si no tiene otras clasificaciones.
     *
     * @param folio El folio de la instrucción a reclasificar.
     * @return ResponseEntity con estado 200 OK si la operación es exitosa, o 500
     *         INTERNAL_SERVER_ERROR si ocurre un error.
     */
    @PutMapping("/reclasificadas/{folio}")
    public ResponseEntity<Void> reclasificarInstruccionMonetaria(@PathVariable String folio) {

        try {
            instruccionService.reclasificarInstruccionMonetaria(folio);
            return ResponseEntity.ok().build(); // Retorna 200 OK
        } catch (Exception e) {
            log.error("Error al reclasificar instruccion monetaria con folio {}: {}", folio, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // Retorna 500
        }
    }

    @PutMapping("/juridicas/reclasificadas/{folio}")
    public ResponseEntity<Void> reclasificarInstruccionJuridica(@PathVariable String folio) {

        try {
            instruccionService.reclasificarInstruccionJuridica(folio);
            return ResponseEntity.ok().build(); // Retorna 200 OK
        } catch (Exception e) {
            log.error("Error al reclasificar instrucción juridica con folio {}: {}", folio, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // Retorna 500
        }
    }

    @PutMapping("/juridicas/finalizar/{folio}")
    public ResponseEntity<Void> actualizarInstruccionJuridica(@PathVariable String folio,
            @RequestParam EstatusInstruccionEnum estatus) {
        instruccionService.actualizarEstatusInstruccionJuridica(folio, estatus);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/juridicas")
    public ResponseEntity<List<InstruccionJuridicaDto>> listarInstruccionesJuridicasNoEnProceso(
            @AuthenticationPrincipal(expression = "claims['sub']") String empleado,
            @RequestParam(defaultValue = "false") boolean isInProceso) throws InterruptedException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        boolean isEL = false;
        if (auth != null && auth.getAuthorities() != null) {
            for (GrantedAuthority ga : auth.getAuthorities()) {
                if (ga != null && ga.getAuthority() != null &&
                        ga.getAuthority().equalsIgnoreCase(Rol.ROLE_EL.name())) {
                    isEL = true;
                    break;
                }
            }
        }

        List<InstruccionJuridicaDto> instrucciones = new ArrayList<>();

        // Lógica original: si es ROLE_EL (jurídico) o cualquier otro, siempre obtiene
        // sus instrucciones
        if (isEL) {
            instrucciones = instruccionService.obtenerInstruccionesJuridicas(empleado);
        } else {
            instrucciones = instruccionService.obtenerInstruccionesJuridicas(empleado);
        }

        return ResponseEntity.ok(instrucciones);
    }
}