package com.bim.seif.repositories;

import com.bim.seif.models.Evento;
import com.bim.seif.models.TipoEvento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventoRepository extends JpaRepository<Evento, TipoEvento> {
}
