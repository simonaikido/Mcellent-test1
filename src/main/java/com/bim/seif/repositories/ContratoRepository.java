package com.bim.seif.repositories;

import com.bim.seif.models.Contrato;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ContratoRepository extends JpaRepository<Contrato, Contrato.ContratoId> {

    List<Contrato> findByFideicomisoFolio(String folio);

    @Query("SELECT c FROM Contrato c WHERE c.id.clienteId = :clienteId AND c.id.fideicomisoFolio = :folio")
    Optional<Contrato> findByClienteAndFolio(@Param("clienteId") Long clienteId,
            @Param("folio") String folio);
}