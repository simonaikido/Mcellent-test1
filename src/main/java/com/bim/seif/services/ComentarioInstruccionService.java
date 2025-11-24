package com.bim.seif.services;

import com.bim.seif.models.ComentarioInstruccion;
import com.bim.seif.models.Instruccion;
import com.bim.seif.models.dto.ComentarioResponseDto;
import com.bim.seif.repositories.ComentarioInstruccionRepository;
import com.bim.seif.repositories.InstruccionRepository;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ComentarioInstruccionService {

    @Autowired
    private ComentarioInstruccionRepository comentarioInstruccionRepository;

    @Autowired
    private InstruccionRepository instruccionRepository; // Necesitas un repositorio para Instruccion si no lo tienes

    public ComentarioResponseDto guardarComentario(String folioInstruccion, String comentario, String empleadoEmail, String tipoComentario) {
        Optional<Instruccion> instruccionOptional = instruccionRepository.findById(folioInstruccion);

        if (instruccionOptional.isPresent()) {
            Instruccion instruccion = instruccionOptional.get();

            ComentarioInstruccion nuevoComentario = new ComentarioInstruccion();
            nuevoComentario.setInstruccion(instruccion);
            nuevoComentario.setComentario(comentario);
            nuevoComentario.setEmpleadoEmail(empleadoEmail);
            nuevoComentario.setTipoComentario(tipoComentario);
            nuevoComentario.setFechaComentario(LocalDateTime.now());
            nuevoComentario.setLeeida(false);

            ComentarioInstruccion comentarioGuardado = comentarioInstruccionRepository.save(nuevoComentario);

            // Mapear la entidad guardada al DTO de respuesta
            ComentarioResponseDto responseDTO = new ComentarioResponseDto();
            responseDTO.setId(comentarioGuardado.getId());
            responseDTO.setComentario(comentarioGuardado.getComentario());
            responseDTO.setLeeida(comentarioGuardado.isLeeida());
            responseDTO.setFechaComentario(comentarioGuardado.getFechaComentario());
            responseDTO.setTipoComentario(comentarioGuardado.getTipoComentario());
            responseDTO.setEmpleadoEmail(comentarioGuardado.getEmpleadoEmail());
            responseDTO.setFolioInstruccion(comentarioGuardado.getInstruccion().getFolio()); // Accedes al folio directamente
            return responseDTO;
        } else {
            throw new RuntimeException("Instrucción con folio " + folioInstruccion + " no encontrada.");
        }
    }

    //MÉTODO para obtener comentarios por folio de instrucción
    public List<ComentarioResponseDto> obtenerComentariosPorFolioInstruccion(String folioInstruccion) {
        // Asumiendo que ComentarioInstruccion tiene un campo 'instruccion'
        // y que Instruccion tiene un campo 'folio'
        List<ComentarioInstruccion> comentarios = comentarioInstruccionRepository.findByInstruccionFolio(folioInstruccion);

        // Mapear la lista de entidades a una lista de DTOs
        return comentarios.stream()
                .map(comentario -> {
                    ComentarioResponseDto dto = new ComentarioResponseDto();
                    dto.setId(comentario.getId());
                    dto.setComentario(comentario.getComentario());
                    dto.setLeeida(comentario.isLeeida());
                    dto.setFechaComentario(comentario.getFechaComentario());
                    dto.setTipoComentario(comentario.getTipoComentario());
                    dto.setEmpleadoEmail(comentario.getEmpleadoEmail());
                    dto.setFolioInstruccion(comentario.getInstruccion() != null ? comentario.getInstruccion().getFolio() : null);
                    return dto;
                })
                .collect(Collectors.toList());
                
    }
}