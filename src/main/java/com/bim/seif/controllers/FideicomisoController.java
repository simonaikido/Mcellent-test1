package com.bim.seif.controllers;

import com.bim.seif.models.dto.RegionDto;
import com.bim.seif.services.FideicomisoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/fideicomisos")
@RequiredArgsConstructor
public class FideicomisoController {

    private final FideicomisoService fideicomisoService;

    @GetMapping("/regiones")
    public ResponseEntity<List<RegionDto>> obtenerRegiones(){
        
        log.info("Solicitud para obtener todas las regiones.");

        List<RegionDto> regiones = fideicomisoService.obtenerRegiones();
        
        log.info("Se recuperaron {} regiones.", regiones.size());
        return ResponseEntity.ok(regiones);
    }

    @PutMapping("/{folio}")
    public ResponseEntity<Void> actualizarFideicomiso(@PathVariable String folio
            , @RequestParam("regionCve") String regionCve
            , @RequestParam("ejecutivoResponsableEmail") String ejecutivoResponsableEmail
            , @RequestParam("instruccionesMonetarias") boolean instruccionesMonetarias
            , @RequestParam("instruccionesJuridicas") boolean instruccionesJuridicas) {
        
        log.info(" Solicitud de actualización para Fideicomiso folio: {}", folio,
            regionCve, ejecutivoResponsableEmail, instruccionesMonetarias, instruccionesJuridicas);
        
        
        fideicomisoService.actualizarFideicomiso(folio, ejecutivoResponsableEmail, regionCve, instruccionesMonetarias, instruccionesJuridicas);
        
        log.info(" Fideicomiso folio {} actualizado con exito.", folio);
        return ResponseEntity.ok().build();

    }
}