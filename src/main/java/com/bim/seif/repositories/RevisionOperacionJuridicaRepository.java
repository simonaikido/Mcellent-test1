package com.bim.seif.repositories;

import com.bim.seif.models.RevisionOperacionJuridica;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RevisionOperacionJuridicaRepository extends JpaRepository<RevisionOperacionJuridica, Long> {
    List<RevisionOperacionJuridica> findByOperacionJuridicaId(long operacionJuridicaId);
}