package com.bim.seif.repositories;

import com.bim.seif.models.EstatusOperacion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EstatusOperacionRepository extends JpaRepository<EstatusOperacion, String> {
    EstatusOperacion findByCve(String cve);

}
