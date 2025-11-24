package com.bim.seif.repositories;

import com.bim.seif.models.ConfiguracionNivelAprobacionMonto;
import com.bim.seif.models.Rol;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NivelAprobacionRepository extends JpaRepository<ConfiguracionNivelAprobacionMonto, String> {

    Optional<ConfiguracionNivelAprobacionMonto> findByRol(Rol rol);

}