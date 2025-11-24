package com.bim.seif.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bim.seif.models.ConfiguracionAprobacionGral;

public interface ConfiguracionAprobacionRepository extends JpaRepository<ConfiguracionAprobacionGral, String> {
}