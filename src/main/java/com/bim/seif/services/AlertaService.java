package com.bim.seif.services;

import com.bim.seif.models.*;
import com.bim.seif.models.dto.NotificacionDto;
import com.bim.seif.models.mappers.NotificacionMapper;
import com.bim.seif.repositories.HistorialNotificacionRepository;
import com.bim.seif.repositories.NotificacionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertaService {

    private final NotificacionRepository notificacionRepository;
    private final HistorialNotificacionRepository historialNotificacionRepository;
    private final NotificacionMapper notificacionMapper;

    public void actualizarNotificacion(TipoNotificacion id,NotificacionDto notificacion){
        Notificacion notificacionEnt = notificacionRepository.findById(id).orElseThrow();

        HistorialNotificacion historial = new HistorialNotificacion();
        historial.setNotificacion(notificacionEnt);
//        historial.setNombre(notificacionEnt.getNombre());
        historial.setTitulo(notificacionEnt.getTitulo());
        historial.setMensaje(notificacionEnt.getMensaje());
        historial.setDesactivada(notificacionEnt.isDesactivada());
        historial.setModificadoPor(notificacionEnt.getModificadoPor());
        historial.setFechaModificacion(notificacionEnt.getFechaModificacion());
        historial.setComentario(notificacionEnt.getComentario());
        historialNotificacionRepository.save(historial);

//        notificacionEnt.setNombre(notificacion.getNombre());
        notificacionEnt.setTitulo(notificacion.getTitulo());
        notificacionEnt.setMensaje(notificacion.getMensaje());
        notificacionEnt.setDesactivada(notificacion.isDesactivada());
        notificacionEnt.setModificadoPor(notificacion.getModificadoPor());
        notificacionEnt.setComentario(notificacion.getComentario());
        notificacionRepository.save(notificacionEnt);
    }

    public List<NotificacionDto> obtenerNotificaciones(){
       return notificacionMapper.toDto(notificacionRepository.findAll());
    }

    public Set<Map<TipoPropiedad, Map<String,String>>> obtenerParametro(TipoNotificacion tn){
        Set<Map<TipoPropiedad,Map<String,String>>> parametros = new HashSet<>();
        for (TipoPropiedad tp : tn.getVariables()) {
            parametros.add(Map.of(tp,
                    Arrays.stream(Propiedad.values()).filter(p->p.getTipoPropiedad()
                            .equals(tp)).collect(Collectors.toMap(k->k.name(), v->v.getDescripcion()))

            ));
        }

        return parametros;

    }

    public void cambiarEstatus(TipoNotificacion cve) {
        Notificacion n = notificacionRepository.findById(cve).orElseThrow();
        n.setDesactivada(!n.isDesactivada());
        notificacionRepository.save(n);
    }
}
