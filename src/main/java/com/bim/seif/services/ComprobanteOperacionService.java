package com.bim.seif.services;

import com.bim.seif.models.Cliente;
import com.bim.seif.models.ComprobanteOperacion;
import com.bim.seif.models.Instruccion;
import com.bim.seif.models.OperacionMonetaria;
import com.bim.seif.models.Propiedad;
import com.bim.seif.models.TipoEvento;
import com.bim.seif.repositories.ClienteRespository;
import com.bim.seif.repositories.ComprobanteOperacionRepository;
import com.bim.seif.repositories.OperacionJuridicaRepository;
import com.bim.seif.repositories.OperacionMonetariaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ComprobanteOperacionService {

    private final ComprobanteOperacionRepository compRepo;
    private final OperacionMonetariaRepository opMonRepo;
    private final OperacionJuridicaRepository opJurRepo;
    private final ClienteRespository clienteRespository;
    private final InstruccionService instruccionService;
    private final EmailService emailService;
    

    @Transactional
    public void asignarAOperaciones(Long comprobanteId, String tipoOperacion, List<Long> operacionIds) {
        ComprobanteOperacion co = compRepo.findById(comprobanteId)
                .orElseThrow(() -> new IllegalArgumentException("Comprobante no encontrado"));

        // si no tiene fecha, la seteamos ahora
        if (co.getFechaCarga() == null) {
            co.setFechaCarga(LocalDateTime.now());
            compRepo.save(co); // persistimos el cambio
        }

        if ("MONETARIA".equalsIgnoreCase(tipoOperacion)) {
            var ops = opMonRepo.findAllById(operacionIds);
            ops.forEach(op -> op.setComprobante(co));
            opMonRepo.saveAll(ops);
        } else if ("JURIDICA".equalsIgnoreCase(tipoOperacion)) {
            var ops = opJurRepo.findAllById(operacionIds);
            ops.forEach(op -> op.setComprobante(co));
            opJurRepo.saveAll(ops);
        } else {
            throw new IllegalArgumentException("tipoOperacion inválido");
        }
    }

    @Transactional
    public ComprobanteOperacion crearYAsignar(String accion, String rutaRelativa, String tipoOperacion, List<Long> operacionIds) {
        // 1) Crear comprobante
        ComprobanteOperacion co = new ComprobanteOperacion();
        co.setRutaComprobante(rutaRelativa);
        co.setStatus(false); // si lo usas como "enviado/pendiente", lo dejamos igual
        co = compRepo.save(co);

        // 2) Asignar a operaciones (tu método existente)
        asignarAOperaciones(co.getIdComprobante(), tipoOperacion, operacionIds);
        
        return co;
    }

    public void verificarYFinalizarInstruccion(List<Long> operacionIds) {
        List<OperacionMonetaria> operaciones = opMonRepo.findAllById(operacionIds);
        if (operaciones == null || operaciones.isEmpty())
            return;
        Instruccion intruccion = operaciones.get(0).getInstruccion().getInstruccion();
        String email = intruccion.getClienteCarga();
        Cliente cliente = clienteRespository.findByEmail(email);

        String folio = intruccion.getFolio();
        String clienteFullName = cliente.getNombre() + " " + cliente.getApellidoPaterno() + " "
                + cliente.getApellidoMaterno();

        if (folio == null || folio.isBlank())
            return;

        List<OperacionMonetaria> todasOps = opMonRepo.findAllByInstruccion_Folio(folio);
        boolean todasConComprobante = !todasOps.isEmpty() &&
                todasOps.stream().allMatch(op -> op.getComprobante() != null);

        if (todasConComprobante) {
            instruccionService.actualizarEstatusInstruccionMonetaria(folio, "FI");
            emailService.enviarCorreo(email, TipoEvento.comprobante_pago,
                    Map.of(
                            Propiedad.instruccion_cliente_email, clienteFullName,
                            Propiedad.instruccion_folio, folio,
                            Propiedad.instruccion_tipo, "MONETARIA",
                            Propiedad.instruccion_fecha_recepcion, intruccion.getFechaAlta().toString(),
                            Propiedad.fideicomiso_alias, intruccion.getFideicomiso().getAlias(),
                            Propiedad.fideicomiso_folio, intruccion.getFideicomiso().getFolio()));
        }
    }
}
