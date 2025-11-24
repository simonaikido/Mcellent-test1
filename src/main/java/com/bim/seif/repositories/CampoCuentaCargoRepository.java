package com.bim.seif.repositories;

import com.bim.seif.models.CampoCuentaCargo;
import com.bim.seif.models.OperacionMonetaria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CampoCuentaCargoRepository extends JpaRepository<CampoCuentaCargo, Long> {
    Optional<CampoCuentaCargo> findByOperacionMonetaria(OperacionMonetaria operacionMonetaria);
    Optional<CampoCuentaCargo> findByCuenta(String cuenta);
    @Query(value = """
        SELECT TOP 1 c.*
        FROM CampoCuentaCargo c
        WHERE REPLACE(REPLACE(REPLACE(c.cuenta, '.', ''), '-', ''), ' ', '') = :numero
        """, nativeQuery = true)
    Optional<CampoCuentaCargo> findByCuentaNormalizada(@Param("numero") String numeroSoloDigitos);
}