package com.bim.seif.repositories;

import com.bim.seif.models.CampoReferencia;
import com.bim.seif.models.OperacionMonetaria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CampoReferenciaRepository extends JpaRepository<CampoReferencia, Long> {
    Optional<CampoReferencia> findByOperacionMonetaria(OperacionMonetaria operacionMonetaria);
}