package com.bim.seif.services;

import com.bim.seif.models.InstruccionClasificada;

import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class InstruccionClasificadaService {

    public List<InstruccionClasificada> listarInstruccionesEnProceso() {
        return null;
    }

    public InstruccionClasificada obtenerInstruccionPorId(Long id) {
        return null;
    }

    public InstruccionClasificada actualizarInstruccion(InstruccionClasificada instruccion) {
        return null;
    }

    public List<InstruccionClasificada> filtrarInstrucciones(String tipo, String region, String empleado,
            LocalDateTime fechaInicio, LocalDateTime fechaFin) {

        return null;
    }

    public List<InstruccionClasificada> buscarInstruccionesPorFideicomiso(String fideicomiso) {
        return null;
    }

    public List<InstruccionClasificada> listarInstruccionesOrdenadasPorFecha() {

        return null;
    }

    public void procesarRechazo(Long instruccionId, String comentarioRechazo) {
        log.info("Procesando rechazo para la instruccion ID: {}, comentario: {}", instruccionId, comentarioRechazo);
    }
}