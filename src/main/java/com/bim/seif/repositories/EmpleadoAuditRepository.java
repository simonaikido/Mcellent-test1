package com.bim.seif.repositories;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.bim.seif.models.EmpleadoAudit;

import feign.Param;

@Repository
public interface EmpleadoAuditRepository extends JpaRepository<EmpleadoAudit, Long> {
    List<EmpleadoAudit> findByAccionInAndCreatedAtBetween(List<String> accion, OffsetDateTime start,
            OffsetDateTime end);

    @Query(value = """
            SELECT uid, accion, actor, ip, user_agent, payload_before, payload_after, created_at, comentario
            FROM dbSEIF.dbo.empleado_audit
            WHERE created_at BETWEEN :start AND :end
            ORDER BY created_at DESC
            """, countQuery = """
            SELECT COUNT(1)
            FROM dbSEIF.dbo.empleado_audit
            WHERE created_at BETWEEN :start AND :end
            """, nativeQuery = true)
    Page<Object[]> buscarAuditoriaCruda(
            @Param("start") OffsetDateTime start,
            @Param("end") OffsetDateTime end,
            Pageable pageable);
}