package com.bim.seif.repositories;

import com.bim.seif.models.Campo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CampoRepository extends JpaRepository<Campo, Long> {
    Optional<Campo> findByOperacionMonetariaId(Long operacionMonetariaId);
}