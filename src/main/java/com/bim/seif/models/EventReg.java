package com.bim.seif.models;

import com.bim.seif.repositories.EventoAuditoriaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.persistence.PostUpdate;

@Component
@RequiredArgsConstructor
public class EventReg {

    private final EventoAuditoriaRepository repository;

    @PostUpdate
    public void auditar(Evento e){
        EventoAuditoria event = new EventoAuditoria();
        event.setEvento(e);
        event.setAsunto(e.getAsunto());
        event.setDesactivado(e.isDesactivado());
        event.setCuerpoCorreo(e.getCuerpoCorreo());
        event.setFechaModificacion(e.getFechaModificacion());
        repository.save(event);
    }


}
