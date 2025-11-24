package com.bim.seif.services;

import com.bim.seif.models.Campo;
import com.bim.seif.repositories.CampoRepository;

import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CampoService {

    private final CampoRepository campoRepository;

    @Autowired
    public CampoService(CampoRepository campoRepository) {
        
        this.campoRepository = campoRepository;
    }

    /**
     * Busca un campo por el ID de operación monetaria
     */
    public Optional<Campo> obtenerPorOperacionMonetariaId(Long operacionMonetariaId) {
        return campoRepository.findByOperacionMonetariaId(operacionMonetariaId);
    }
    /**
     * Busca un campo por su ID (si lo necesitas en el futuro)
     */
    public Campo obtenerPorId(Long id) {
        return campoRepository.findById(id).orElse(null);
    }
}
