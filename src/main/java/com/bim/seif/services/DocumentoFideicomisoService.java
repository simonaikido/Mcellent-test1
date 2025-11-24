package com.bim.seif.services;

import com.bim.seif.models.DocumentoFideicomiso;
import com.bim.seif.repositories.DocumentoFideicomisoRepository; // Necesitarás crear este repositorio
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentoFideicomisoService {

    @Autowired
    private DocumentoFideicomisoRepository documentoFideicomisoRepository;

    public List<DocumentoFideicomiso> getDocumentosByFolioFideicomiso(String fideicomisoFolio) {
        return documentoFideicomisoRepository.findByFideicomisoFolio(fideicomisoFolio);
    }
}