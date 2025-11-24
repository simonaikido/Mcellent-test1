package com.bim.seif.repositories;

import com.bim.seif.models.ConfiguracionOperacionMonetaria;
import com.bim.seif.models.ConfiguracionOperacionMonetariaId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConfiguracionOperacionMonetariaRepository extends JpaRepository<ConfiguracionOperacionMonetaria, ConfiguracionOperacionMonetariaId> {
    List<ConfiguracionOperacionMonetaria> findByTipoOperacionMonetariaCve(String tipoOperacionCve);
}