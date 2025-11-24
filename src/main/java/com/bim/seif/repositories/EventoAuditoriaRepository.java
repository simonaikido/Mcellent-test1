package com.bim.seif.repositories;

import com.bim.seif.models.EventoAuditoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventoAuditoriaRepository extends JpaRepository<EventoAuditoria,Long> {
}
