package com.bim.seif.repositories;

import com.bim.seif.models.DocumentoFideicomiso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DocumentoFideicomisoRepository extends JpaRepository<DocumentoFideicomiso, Long> {
    @Query(value = "SELECT TOP 5 * FROM documento_fideicomiso WHERE fideicomiso_folio = :folio ORDER BY fecha_carga DESC", nativeQuery = true)
    List<DocumentoFideicomiso> findByFideicomisoFolio(@Param("folio") String fideicomisoFolio);
}