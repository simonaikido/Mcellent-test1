// src/main/java/com/bim/seif/repositories/ComentarioInstruccionRepository.java
package com.bim.seif.repositories;

import com.bim.seif.models.ComentarioInstruccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComentarioInstruccionRepository extends JpaRepository<ComentarioInstruccion, Long> {
    List<ComentarioInstruccion> findByInstruccionFolio(String instruccionFolio);
}