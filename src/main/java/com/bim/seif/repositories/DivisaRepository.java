package com.bim.seif.repositories;

import com.bim.seif.models.Divisa;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DivisaRepository extends JpaRepository<Divisa, String> {
    Optional<Divisa> findByCve(String cve);
}
