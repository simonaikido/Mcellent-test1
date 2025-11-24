package com.bim.seif.controllers;

import com.bim.seif.models.dto.ClienteDto;
import com.bim.seif.models.dto.ClienteRequestDto;
import com.bim.seif.models.dto.FideicomisoDto;
import com.bim.seif.services.CustomerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    @PostMapping("/reportes")
    public ResponseEntity<List<ClienteDto>> obtenerClientesRegistrados(
            @RequestBody ClienteDto cliente,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate desde,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate hasta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(defaultValue = "id") String sortBy

    ) {
        

        List<ClienteDto> clientes = customerService
                .obtenerClientes(desde, hasta, cliente, page, size, Sort.by(sortBy));
        
        log.info("Reporte de clientes completado. Resultados: {}", clientes.size());
        
        return ResponseEntity.ok(clientes);
    }

    @GetMapping("/fideicomisos/{folio}")
    public ResponseEntity<FideicomisoDto> obtenerClientesGestorFideicomiso(
            @PathVariable String folio

    ) {
        
        FideicomisoDto fideicomiso = customerService
                .obtenerClientesFideicomiso(folio);
        
        log.info("Clientes gestores recuperados para fideicomiso: {}", folio);
        
        return ResponseEntity.ok(fideicomiso);
    }


    @PostMapping
    public ResponseEntity<ClienteDto> registrarCliente(
            @RequestBody ClienteRequestDto clienteRequest) throws Exception {
        
        
        ClienteDto cliente;
        try {
             cliente = customerService
                    .registrarCliente(clienteRequest);
        } catch (Exception e) {
            log.error("Fallo al registrar cliente con email: {}. Causa: {}", clienteRequest.getEmail(), e.getMessage());
            throw e;
        }

        log.info("Cliente registrado con exito. ID: {}", cliente.getId());
        return ResponseEntity.ok(cliente);
    }

    @DeleteMapping("/{clienteEmail}")
    public ResponseEntity<Void> desactivarCliente(
            @PathVariable String clienteEmail, @RequestParam(defaultValue = "true") boolean disabled) {
        
        
        customerService
                .desactivarCliente(clienteEmail, disabled);
        
        log.info("Cliente {} {} con exito.", clienteEmail, (disabled ? "desactivado" : "activado"));
        return ResponseEntity.ok().build();
    }
}