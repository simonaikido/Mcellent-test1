package com.bim.seif.services;

import com.bim.seif.models.Actividad;
import com.bim.seif.models.InstruccionMonetaria;
import com.bim.seif.models.dto.ActividadDto; // Importa el DTO
import com.bim.seif.models.mappers.ActividadMapper; // Importa el Mapper
import com.bim.seif.repositories.ActividadRepository;
import com.bim.seif.repositories.InstruccionMonetariaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActividadService {

    private final ActividadRepository actividadRepository;
    private final InstruccionMonetariaRepository instruccionMonetariaRepository;

    public Actividad registrarActividad(String folioInstruccion, String descripcion, String usuario) {
            log.info("Registrando actividad para la instruccion con folio: {}", folioInstruccion);
        // ... (Este método se mantiene igual, ya que registra la entidad Actividad)
        Optional<InstruccionMonetaria> instruccionOptional = instruccionMonetariaRepository.findById(folioInstruccion);
        if (instruccionOptional.isPresent()) {
            InstruccionMonetaria instruccion = instruccionOptional.get();

            Actividad actividad = new Actividad();
            actividad.setInstruccion(instruccion);
            actividad.setFechaHora(LocalDateTime.now());
            actividad.setDescripcion(descripcion);
            actividad.setUsuario(usuario);

         // CHECKMARX-FP: Safe operation — using JPA save() on managed entity 'actividad'. 
         // No dynamic SQL or unparameterized query involved; repository extends JpaRepository.
         return actividadRepository.save(actividad);
        } else {
            log.error("Error: Instruccion con folio {} no encontrada. No se pudo registrar la actividad.", folioInstruccion);
            return null;
        }
    }

    public List<ActividadDto> obtenerActividadesPorFolioInstruccion(String folioInstruccion) {
        log.info("Solicitud para obtener actividades por folio de instruccion: {}", folioInstruccion);
        List<Actividad> actividades = actividadRepository.findByInstruccion_FolioOrderByFechaHoraDesc(folioInstruccion);
        // Convierte las entidades Actividad a DTOs antes de devolverlas
        return ActividadMapper.toDto(actividades);
    }
}