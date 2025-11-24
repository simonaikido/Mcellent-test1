package com.bim.seif.services;

import com.bim.seif.models.DocumentoInstruccion;
import com.bim.seif.repositories.DocumentoInstruccionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentoInstruccionService {

    private final DocumentoInstruccionRepository documentoInstruccionRepository;

    /**
     * Obtiene todos los documentos de instrucción asociados a un folio dado.
     * @param folioInstruccion El folio de la instrucción.
     * @return Una lista de DocumentoInstruccion.
     */
    public List<DocumentoInstruccion> getDocumentosByFolioInstruccion(String folioInstruccion) {
        return documentoInstruccionRepository.findByInstruccion_Folio(folioInstruccion);
    }

    // Aquí podrías añadir otros métodos si necesitas guardar, actualizar o eliminar documentos.
    public DocumentoInstruccion saveDocumentoInstruccion(DocumentoInstruccion documento) {
        return documentoInstruccionRepository.save(documento);
    }
}