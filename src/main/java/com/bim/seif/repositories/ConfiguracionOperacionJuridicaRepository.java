package com.bim.seif.repositories;

import com.bim.seif.models.ConfiguracionOperacionJuridica;
import com.bim.seif.models.ConfiguracionOperacionJuridicaId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConfiguracionOperacionJuridicaRepository extends JpaRepository<ConfiguracionOperacionJuridica, ConfiguracionOperacionJuridicaId> {

    List<ConfiguracionOperacionJuridica> findByRolAndRevision(String rol, boolean revision);
    List<ConfiguracionOperacionJuridica> findByRolAndAprobacion(String rol, boolean aprobacion);

}