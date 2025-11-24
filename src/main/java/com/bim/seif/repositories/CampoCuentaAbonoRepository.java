package com.bim.seif.repositories;

import com.bim.seif.models.CampoCuentaAbono;
import com.bim.seif.models.OperacionMonetaria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CampoCuentaAbonoRepository extends JpaRepository<CampoCuentaAbono, Long> {
    Optional<CampoCuentaAbono> findByOperacionMonetaria(OperacionMonetaria operacionMonetaria);
}