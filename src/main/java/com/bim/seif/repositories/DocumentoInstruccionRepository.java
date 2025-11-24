package com.bim.seif.repositories;

import com.bim.seif.models.DocumentoInstruccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DocumentoInstruccionRepository extends JpaRepository<DocumentoInstruccion, Long> {

    List<DocumentoInstruccion> findByInstruccion_Folio(String instruccionFolio);
}