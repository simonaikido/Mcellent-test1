package com.bim.seif.repositories;

import com.bim.seif.models.CuentaAbono;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface CuentaRepository extends JpaRepository<CuentaAbono, Long>, JpaSpecificationExecutor<CuentaAbono> {
    List<CuentaAbono> findByFideicomisoFolio(String folio);
    List<CuentaAbono> findByFideicomisoFolioAndFechaBajaIsNull(String folio);
    List<CuentaAbono> findByCuenta(Long cuenta);
}