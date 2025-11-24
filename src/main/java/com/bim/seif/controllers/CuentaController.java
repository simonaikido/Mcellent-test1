package com.bim.seif.controllers;

import com.bim.seif.models.CuentaAbono;
import com.bim.seif.models.dto.CuentaAbonoDto;
import com.bim.seif.models.dto.CuentaCargoDto;
import com.bim.seif.services.CuentaService;
import com.bim.seif.utils.Validators;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/cuentas")
@RequiredArgsConstructor
public class CuentaController {

    @Autowired
    private final CuentaService cuentaService;

    @GetMapping("abono/{fideicomisoFolio}")
    public ResponseEntity<List<CuentaAbonoDto>> obtenerCuentasAbono(
            @PathVariable("fideicomisoFolio") String fideicomisoFolio) {
        return ResponseEntity.ok(cuentaService.obtenerCuentas(fideicomisoFolio));
    }

     @GetMapping("abono/cuenta/{cuenta}")
    public ResponseEntity<List<CuentaAbonoDto>> obtenerCuentasAbonoByCuenta(
            @PathVariable("cuenta") String cuenta) {
                Long keyCuenta = Long.parseLong(cuenta);
        return ResponseEntity.ok(cuentaService.obtenerCuentasAbonoByCuenta(keyCuenta));
    }

    @GetMapping("abono/mantenimiento/{fideicomisoFolio}")
    public ResponseEntity<List<CuentaAbonoDto>> obtenerCuentasMantenimiento(
            @PathVariable("fideicomisoFolio") String fideicomisoFolio) {
        return ResponseEntity.ok(cuentaService.obtenerCuentasMantenimiento(fideicomisoFolio));
    }

    @PostMapping
    public ResponseEntity<List<CuentaAbonoDto>> obtenerCuentas(
            @RequestBody CuentaAbonoDto cuentaAbono,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate desde,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate hasta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @AuthenticationPrincipal(expression = "claims['sub']") String username,
            @RequestParam(defaultValue = "cuenta") String sortBy) {
    	
    	Validators.sanitizeUsername(username);

        // Validaciones mínimas (opcional)
        if (desde != null && hasta != null && hasta.isBefore(desde)) {
            return ResponseEntity.badRequest().build();
        }
        if (page < 0)
            page = 0;
        if (size <= 0)
            size = 15;

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));

        List<CuentaAbonoDto> resultado = cuentaService.buscarCuentas(username, cuentaAbono, desde, hasta, pageable);

        return ResponseEntity.ok(resultado);
    }

    @GetMapping("cargo/{fideicomisoFolio}")
    public ResponseEntity<List<CuentaCargoDto>> obtenerCuentasCargo(
            @PathVariable("fideicomisoFolio") String fideicomisoFolio) {
        return ResponseEntity.ok(cuentaService.obtenerCuentasCargo(fideicomisoFolio));
    }

    @PutMapping
    public ResponseEntity<Void> actualizarCuenta(@RequestBody CuentaAbono cuenta) {
        cuentaService.actualizarCuenta(cuenta);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{cuenta}")
    public ResponseEntity<Void> desactivarCuenta(@PathVariable Long cuenta,
            @RequestParam(defaultValue = "true") boolean desactiva) {
        cuentaService.desactivarCuenta(cuenta, desactiva);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/registrar")
    public ResponseEntity<CuentaAbono> registrarCuenta(@RequestBody CuentaAbono cuenta,
            @RequestParam("folioFideicomiso") String folioFideicomiso) {
        CuentaAbono nuevaCuenta = cuentaService.registrarCuenta(cuenta, folioFideicomiso);
        return ResponseEntity.ok(nuevaCuenta);
    }

    @PutMapping("{id}/comprobantes")
    public ResponseEntity<Void> actualizarComprobanteEdoCta(@PathVariable long cuenta,
            @RequestParam("rutaComprobante") String rutaComprobante) {
        cuentaService.registrarComprobante(cuenta, rutaComprobante);
        return ResponseEntity.ok().build();
    }
}
