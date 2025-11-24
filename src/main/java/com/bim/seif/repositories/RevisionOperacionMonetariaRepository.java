package com.bim.seif.repositories;

import com.bim.seif.models.RevisionOperacionMonetaria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.transaction.Transactional;
import java.util.List;

public interface RevisionOperacionMonetariaRepository extends JpaRepository<RevisionOperacionMonetaria, Long> {
    // Nuevo método para eliminar revisiones por operacion_monetaria_id
    @Modifying
    @Transactional
    @Query("DELETE FROM RevisionOperacionMonetaria r WHERE r.operacionMonetaria.id IN :operacionMonetariaIds") // AND
                                                                                                               // r.aprobada
                                                                                                               // =
                                                                                                               // FALSE
                                                                                                               // AND
                                                                                                               // r.omitida
                                                                                                               // =
                                                                                                               // FALSE
    void deleteByOperacionMonetariaIdIn(@Param("operacionMonetariaIds") List<Long> operacionMonetariaIds);

    @Modifying
    @Query("delete from RevisionOperacionMonetaria r where r.operacionMonetaria.id = :opId")
    int deleteByOperacionId(@Param("opId") Long operacionId);

    @Modifying
    @Query("""
            update RevisionOperacionMonetaria r
               set r.omitida = true
             where r.operacionMonetaria.id in :operacionIds
            """)
    int marcarOmitidasPorOperacionIds(@Param("operacionIds") List<Long> operacionIds);
}