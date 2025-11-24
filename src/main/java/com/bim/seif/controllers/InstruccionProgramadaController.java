package com.bim.seif.controllers;

import com.bim.seif.models.DiasFestivos;
import com.bim.seif.models.dto.InstruccionProgramadaDto;
import com.bim.seif.models.dto.InstruccionProgramadaResponseDto;
import com.bim.seif.models.dto.TipoOperacionMonetariaProgramadaDto;
import com.bim.seif.services.InstruccionProgramadaService;
import com.bim.seif.services.InstruccionService;
import com.bim.seif.utils.JSONUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/instrucciones")
@RequiredArgsConstructor
public class InstruccionProgramadaController {

    @Autowired
    InstruccionProgramadaService instruccionProgramadaService;

    @Autowired
    InstruccionService instruccionService;

    @GetMapping("/programada/{responsable}")
    public ResponseEntity<List<InstruccionProgramadaResponseDto>> obtenerInstruccionesProgramadas(
            @PathVariable String responsable) {

        List<InstruccionProgramadaResponseDto> instrucciones = instruccionProgramadaService
                .obtenerInstruccionesProgramadas(responsable);

        log.info("Se recuperaron {} instrucciones programadas para responsable: {}", instrucciones.size(), responsable);
        return ResponseEntity.ok(instrucciones);
    }

    @PostMapping("/programada/{id}")
    public ResponseEntity<Void> guardarInstruccionProgramada(
            @PathVariable String id,
            @RequestParam(name = "rmonetaria", required = false) String responsableMonetaria,
            @RequestParam(name = "rjuridica", required = false) String responsableJuridica) {

        instruccionProgramadaService.guardarInstruccionProgramada(id, responsableMonetaria, responsableJuridica);

        log.info("Responsables asignados a instruccion programada ID: {}", id);
        return ResponseEntity.ok().build();
    }

    @PostMapping(path = "/programada/cancelar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> cancelarInstruccionProgramada(
            @RequestPart(value = "folio") String folio,
            @RequestPart(value = "comentario") String comentario,
            @RequestPart("file") MultipartFile file) {

        System.out.println("Folio: " + folio);
        System.out.println("Comentario: " + comentario);
        System.out.println("Archivo: " + file.getOriginalFilename());

        instruccionProgramadaService.cancelarInstruccionProgramada(folio, comentario, file);

        log.info("Instrucción programada folio {} cancelada con exito.", folio);
        return ResponseEntity.ok(JSONUtils
                .covertObjectToJSON(Collections.singletonMap("Exito", "Instruccion programada cancelada con exito")));
    }

    @PostMapping("/programada")
    @PreAuthorize("hasAnyRole('EA','EL','MC','CI','GA','GL')") // Ajusta los roles permitidos según tu política
    public ResponseEntity<String> guardarInstruccionProgramadaProg(
            @RequestBody InstruccionProgramadaDto instruccionProgramadaProg,
            @AuthenticationPrincipal Jwt jwt) throws InterruptedException {

        // Usuario/rol desde el token
        final String usuario = jwt != null ? jwt.getSubject() : "desconocido";
        final String rol = jwt != null ? jwt.getClaimAsString("p") : null;

        // Logs útiles para auditoría y debugging
        log.info("[/programada] Solicitud de registro por usuario={} rol={}", usuario, rol);
        log.debug("[/programada] Payload recibido: {}", instruccionProgramadaProg);

        // Validaciones mínimas (ajusta a tus reglas de negocio)
        if (instruccionProgramadaProg == null) {
            log.warn("Payload nulo en /programada por {}", usuario);
            return ResponseEntity.badRequest().body(
                    JSONUtils.covertObjectToJSON(Map.of("mensaje", "Payload requerido")));
        }

        try {
            // Si necesitas rastrear quién creó, también puedes pasarlo al service
            instruccionProgramadaService
                    .guardarInstruccionProgramadaProg(instruccionProgramadaProg);

            log.info("Instrucción programada registrada con éxito por {}", usuario);
            return ResponseEntity.ok(
                    JSONUtils.covertObjectToJSON(Collections.singletonMap(
                            "Exito", "Instruccion programada guardada con exito")));

        } catch (IllegalArgumentException ex) {
            // Errores de validación del servicio
            log.warn("Validación en service al registrar instrucción programada: {}", ex.getMessage());
            return ResponseEntity.badRequest().body(
                    JSONUtils.covertObjectToJSON(Map.of("mensaje", ex.getMessage())));

        } catch (Exception ex) {
            // Cualquier otro error
            log.error("Error al registrar instrucción programada por {}: {}", usuario, ex.getMessage(), ex);
            return ResponseEntity.status(500).body(
                    JSONUtils.covertObjectToJSON(Map.of("mensaje", "Error interno al guardar instrucción programada")));
        }
    }

    @PutMapping("/programada/{folio}")
    public ResponseEntity<Void> reclasificarInstruccionMonetaria(@PathVariable String folio) {
        try {
            instruccionProgramadaService.reclasificarInstruccionProgramada(folio);
            log.info("Instruccion programada folio {} reclasificada con exito.", folio);
            return ResponseEntity.ok().build(); // Retorna 200 OK
        } catch (Exception e) {
            System.err
                    .println("Error al reclasificar instruccion programada con folio " + folio + ": " + e.getMessage());
            log.error("Error al reclasificar instruccion programada con folio {}: {}", folio, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // Retorna 500
        }
    }

    @GetMapping("/programada/tipos")
    public ResponseEntity<List<TipoOperacionMonetariaProgramadaDto>> obtenerTiposOperacion() {

        List<TipoOperacionMonetariaProgramadaDto> tipos = instruccionProgramadaService
                .obtenerTiposOperacionProgramada();

        log.info("Se recuperaron {} tipos de operacion programada.", tipos.size());
        return ResponseEntity.ok(tipos);
    }

    @GetMapping("/programada/diasfestivos")
    public ResponseEntity<List<DiasFestivos>> obtenerDiasFestivos() {

        DiasFestivos diasFestivos = new DiasFestivos();
        diasFestivos.setFecha(LocalDateTime.now());
        diasFestivos.setMotivo("");
        DiasFestivos diasFestivos2 = new DiasFestivos();
        diasFestivos2.setFecha(LocalDateTime.of(2025, 3, 1, 12, 15));
        diasFestivos2.setMotivo("Año nuevo;");

        List<DiasFestivos> diasFestivosResp = instruccionProgramadaService.obtenerDiasFestivos();
        diasFestivosResp.add(diasFestivos);
        diasFestivosResp.add(diasFestivos2);

        log.info("Se recuperaron y agregaron {} dias festivos.", diasFestivosResp.size());
        return ResponseEntity.ok(diasFestivosResp);
    }

    @GetMapping("/programada/correo/{folio}")
    public ResponseEntity<List<String>> obtenerCorreosFideicomiso(@PathVariable String folio) {

        List<String> destinatarios = instruccionProgramadaService.obtenerCorreosFideicomiso(folio);

        log.info("Se recuperaron {} correos de fideicomiso para folio: {}", destinatarios.size(), folio);
        return ResponseEntity.ok(destinatarios);
    }
}