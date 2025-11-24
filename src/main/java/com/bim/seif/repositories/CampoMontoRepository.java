package com.bim.seif.repositories;

import com.bim.seif.models.CampoMonto;
import com.bim.seif.models.OperacionMonetaria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CampoMontoRepository extends JpaRepository<CampoMonto, Long> {
    Optional<CampoMonto> findByOperacionMonetaria(OperacionMonetaria operacionMonetaria);
}