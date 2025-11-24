package com.bim.seif.services;

import com.bim.seif.models.*;
import com.bim.seif.models.dto.EventoDto;
import com.bim.seif.models.mappers.EventoMapper;
import com.bim.seif.repositories.EventoAuditoriaRepository;
import com.bim.seif.repositories.EventoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventoService {

    private final EventoRepository eventoRepository;
    private final EventoAuditoriaRepository eventoModificacionRepository;
    private final EventoMapper eventoMapper;

    public List<EventoDto> obtenerEventos() {
        List<Evento> eventos = eventoRepository.findAll();
        return eventoMapper.toDto(eventos);
    }

    public EventoDto obtenerEvento(TipoEvento id) {
        Evento evento = eventoRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("No existe evento: " + id));

        if (evento.isDesactivado()) {
            throw new IllegalStateException("El evento " + id + " está desactivado");
        }
        return eventoMapper.toDto(evento);
    }

    public void actualizarEvento(TipoEvento cve, EventoDto dto) {
        Evento e = eventoRepository.findById(cve).orElseThrow();
        EventoAuditoria event = new EventoAuditoria();
        event.setEvento(e);
        event.setAsunto(e.getAsunto());
        event.setComentario(e.getComentario());
        event.setDesactivado(e.isDesactivado());
        event.setCuerpoCorreo(e.getCuerpoCorreo());
        event.setFechaModificacion(e.getFechaModificacion());
        event.setModificadoPor(e.getModificadoPor());
        eventoModificacionRepository.save(event);

        e.setAsunto(dto.getAsunto());
        e.setDesactivado(dto.isDesactivado());
        e.setCuerpoCorreo(dto.getCuerpoCorreo());
        e.setComentario(dto.getComentario());
        e.setFechaModificacion(dto.getFechaModificacion());
        e.setModificadoPor(dto.getModificadoPor());
        eventoRepository.saveAndFlush(e);

    }

    public void cambiarEstatus(TipoEvento cve) {
        Evento evento = eventoRepository.findById(cve).orElseThrow();
        evento.setDesactivado(!evento.isDesactivado());
        eventoRepository.save(evento);
    }

    public Set<Map<TipoPropiedad, Map<String, String>>> obtenerParametros(TipoEvento te) {

        Set<Map<TipoPropiedad, Map<String, String>>> parametros = new HashSet<>();

        for (TipoPropiedad tp : te.getVariables()) {

            parametros.add(Map.of(tp,
                    Arrays.stream(Propiedad.values()).filter(p -> p.getTipoPropiedad()
                            .equals(tp)).collect(Collectors.toMap(k -> k.name(), v -> v.getDescripcion()))));
        }

        return parametros;
        // return te.getVariables().stream().map( v ->
        // Map.of(v,v.getVariables())).collect(Collectors.toSet());

    }
}
