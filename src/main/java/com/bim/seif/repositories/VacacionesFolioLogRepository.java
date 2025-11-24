package com.bim.seif.repositories;

import com.bim.seif.models.VacacionesFolioLog;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VacacionesFolioLogRepository extends JpaRepository<VacacionesFolioLog, Long> {
    
    List<VacacionesFolioLog> findByAsignacionIdAndValorNuevo(Long asignacionId, String valorNuevo);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE VacacionesFolioLog v
           SET v.valorNuevo = :nuevo,
               v.movedAt = :movedAt
         WHERE v.id IN :ids
    """)
    int marcarAplicados(@Param("ids") List<Long> ids,
                        @Param("nuevo") String nuevo,
                        @Param("movedAt") OffsetDateTime movedAt);
}