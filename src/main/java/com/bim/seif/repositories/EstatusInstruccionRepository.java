package com.bim.seif.repositories;

import com.bim.seif.models.EstatusInstruccion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EstatusInstruccionRepository extends JpaRepository<EstatusInstruccion, String> {
    EstatusInstruccion findByCve(String cve);
}
