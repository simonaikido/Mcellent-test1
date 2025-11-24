package com.bim.seif.controllers;


import com.bim.seif.models.dto.ComentarioResponseDto;
import com.bim.seif.services.ComentarioInstruccionService;

import lombok.extern.slf4j.Slf4j;

import com.bim.seif.models.dto.ComentarioRequestDto; 
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/comentarios")
public class ComentarioInstruccionController {

    @Autowired
    private ComentarioInstruccionService comentarioInstruccionService;

    @GetMapping("/instruccion/{folioInstruccion}")
    public ResponseEntity<List<ComentarioResponseDto>> obtenerComentariosPorFolioInstruccion(
            @PathVariable String folioInstruccion) {
        
        
        List<ComentarioResponseDto> comentarios = comentarioInstruccionService.obtenerComentariosPorFolioInstruccion(folioInstruccion);

        if (comentarios.isEmpty()) {
            log.info("No se encontraron comentarios para folioInstruccion: {}", folioInstruccion);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT); // O HttpStatus.NOT_FOUND si prefieres
        } else {
            log.info("Se recuperaron {} comentarios para folioInstruccion: {}", comentarios.size(), folioInstruccion);
            return new ResponseEntity<>(comentarios, HttpStatus.OK);
        }
    }

    @PostMapping("/instruccion/{folioInstruccion}")
    public ResponseEntity<ComentarioResponseDto> agregarComentarioAInstruccion( // Cambia el tipo de retorno
                                                                               @PathVariable String folioInstruccion,
                                                                               @RequestBody ComentarioRequestDto requestDTO) {

        try {
            ComentarioResponseDto nuevoComentario = comentarioInstruccionService.guardarComentario( // Recibe el DTO
                    folioInstruccion,
                    requestDTO.getComentario(),
                    requestDTO.getEmpleadoEmail(),
                    requestDTO.getTipoComentario()
            );
            
            log.info("Comentario registrado con exito para folio: {}. ID: {}", folioInstruccion, nuevoComentario.getId());
            return new ResponseEntity<>(nuevoComentario, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            log.error("Fallo al guardar comentario para folioInstruccion: {}. Causa: {}", folioInstruccion, e.getMessage());
            // Puedes devolver un DTO de error o simplemente el estado HTTP
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }
}