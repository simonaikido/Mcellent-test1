package com.bim.seif.services;

import com.bim.seif.models.*;
import com.bim.seif.models.dto.*;
import com.bim.seif.models.mappers.SolicitudTipoCambioMapper;
import com.bim.seif.repositories.CampoMontoRepository;
import com.bim.seif.repositories.InstruccionMonetariaRepository;
import com.bim.seif.repositories.RevisionOperacionMonetariaRepository;
import com.bim.seif.repositories.SolicitudTipoCambioRepository;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.EntityNotFoundException;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;

@Slf4j
@Service
public class SolicitudTipoCambioService {

    @Autowired
    private SolicitudTipoCambioRepository solicitudTipoCambioRepository;
    @Autowired
    private CampoMontoRepository campoMontoRepository;
    @Autowired
    private InstruccionMonetariaRepository instruccionMonetariaRepository;
    @Autowired
    private RevisionOperacionMonetariaRepository revisionOperacionMonetariaRepository;
    @Autowired
    private EmpleadoService empleadoService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private SolicitudTipoCambioMapper solicitudTipoCambioMapper;

    public List<SolicitudTipoCambioDto> obtenerTodasLasSolicitudes() {
        List<SolicitudTipoCambio> solicitudes = solicitudTipoCambioRepository.findAllNoRechazadas();
        return solicitudTipoCambioMapper.toDto(solicitudes);
    }

    @Transactional
    public void rechazar(Long id) {
        SolicitudTipoCambio solicitud = solicitudTipoCambioRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Solicitud no encontrada"));

        solicitud.setFechaRechazo(LocalDateTime.now());
        solicitudTipoCambioRepository.save(solicitud);

    }

    public SolicitudTipoCambio crearSolicitudTipoCambio(SolicitudTipoCambio solicitud) {
     // CHECKMARX-FP: Operación segura — uso de JPA save() con SQL parametrizado.
     // Los campos de 'solicitud' no se utilizan en SQL dinámico.
     // Persistencia gestionada por Spring Data JPA.
     return solicitudTipoCambioRepository.save(solicitud);

    }

    // Service
    public List<SolicitudCuentaResumenDto> obtenerPorFolio(String folio) {
        List<Object[]> rs = solicitudTipoCambioRepository.findSolicitudesByFolio(folio);
        return rs.stream().map(this::toResumenDto).collect(Collectors.toList());
    }

    private SolicitudCuentaResumenDto toResumenDto(Object[] r) {
        String contacto = asString(r, 0);
        String cuenta = asString(r, 1);
        String banco = asString(r, 2);
        String beneficiario = asString(r, 3);
        String rfc = asString(r, 4);
        String direccion = asString(r, 5);
        String divisaCve = asString(r, 6);
        boolean nacional = asBoolean(r, 7); // nunca null → false por defecto
        Integer tipoCambio = asInteger(r, 8);
        String descripcion = asString(r, 9);

        return new SolicitudCuentaResumenDto(
                contacto, cuenta, banco, beneficiario, rfc, direccion, divisaCve, nacional, tipoCambio, descripcion);
    }

    private String asString(Object[] r, int idx) {
        return (idx < r.length && r[idx] != null) ? r[idx].toString() : null;
    }

    private boolean asBoolean(Object[] r, int idx) {
        if (idx >= r.length || r[idx] == null)
            return false;
        Object o = r[idx];
        if (o instanceof Boolean)
            return (Boolean) o;
        if (o instanceof Number)
            return ((Number) o).intValue() != 0;
        String s = o.toString().trim();
        // acepta "1", "true", "Y", etc.
        return !"0".equals(s) && !"false".equalsIgnoreCase(s) && !"N".equalsIgnoreCase(s);
    }

    private Integer asInteger(Object[] r, int idx) {
        if (idx >= r.length || r[idx] == null)
            return null;
        Object o = r[idx];
        if (o instanceof Number)
            return ((Number) o).intValue();
        try {
            return new java.math.BigDecimal(o.toString()).intValue();
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    @Transactional(readOnly = false)
    public void actualizarPacto(Long id, PactarTipoCambioRequest req, String validadorEmail) {
        SolicitudTipoCambio s = solicitudTipoCambioRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Solicitud no encontrada"));
        System.out.println("[PACTO][REQ] tipoCambio=" + req.getTipoCambio()
                + ", contacto=" + req.getContacto()
                + ", claveLlamada=" + req.getClaveLlamada());

        System.out.println("[PACTO][BEFORE] id=" + s.getId()
                + ", tipoCambio=" + s.getTipoCambio()
                + ", contacto=" + s.getContacto()
                + ", claveLlamada=" + s.getClaveLlamada());

        Integer nuevoTC = req.getTipoCambio();
        Integer actualTC = s.getTipoCambio();
        boolean cambioTipoCambio = !java.util.Objects.equals(nuevoTC, actualTC);

        // Actualiza siempre estos campos en la solicitud (en memoria)
        s.setContacto(req.getContacto());
        s.setClaveLlamada(req.getClaveLlamada());
        s.setTipoCambio(nuevoTC != null ? nuevoTC : s.getTipoCambio());
        s.setFechaFinalizacion(LocalDateTime.now());

        // Si cambió el tipo de cambio => actualiza CampoMonto + invalida aprobación +
        // borra revisiones
        if (cambioTipoCambio) {
            OperacionMonetaria op = s.getOperacion();
            if (op == null) {
                System.out.println("[PACTO][WARN] La solicitud no tiene OperacionMonetaria asociada.");
            } else {
                // --- 1) CampoMonto ---
                CampoMonto cm = null;
                try {
                    if (op.getCampos() != null) {
                        cm = op.getCampos().stream()
                                .filter(c -> c instanceof CampoMonto)
                                .map(c -> (CampoMonto) c)
                                .findFirst().orElse(null);
                    }
                } catch (Exception e) {
                    System.out.println("[PACTO][INFO] No se pudo navegar op.getCampos(): " + e.getMessage());
                }
                if (cm == null) {
                    cm = campoMontoRepository.findByOperacionMonetaria(op).orElse(null);
                }
                if (cm != null && nuevoTC != null) {
                    System.out.println("[PACTO][CM] Monto actual=" + cm.getMonto()
                            + ", Divisa=" + (cm.getDivisa() != null ? cm.getDivisa().getCve() : "null")
                            + ", TipoCambioAnterior=" + cm.getTipoCambio());
                    cm.setTipoCambio(nuevoTC);
                    campoMontoRepository.saveAndFlush(cm);
                    System.out.println("[PACTO][CM][SAVED] NuevoTipoCambio=" + cm.getTipoCambio());
                }

                // --- 2) Instrucción: asigna validador y limpia fecha ---
                InstruccionMonetaria instr = op.getInstruccion();
                if (instr != null) {
                    instr.setValidadorEmail(validadorEmail);
                    instr.setFechaAprobacion(null);
                    instruccionMonetariaRepository.saveAndFlush(instr);
                    System.out.println("[PACTO][INSTR][INVALIDADA] folio=" + instr.getFolio());
                } else {
                    System.out.println("[PACTO][INSTR][WARN] Operación sin instrucción ligada.");
                }

                // --- 3) Borrar revisiones ---
                int borradas = revisionOperacionMonetariaRepository.deleteByOperacionId(op.getId());
                System.out.println("[PACTO][REV][DELETED] total=" + borradas + " para operacion_id=" + op.getId());
                
            }
        }

        System.out.println("[PACTO][TO-SAVE] id=" + s.getId()
                + ", tipoCambio=" + s.getTipoCambio()
                + ", contacto=" + s.getContacto()
                + ", claveLlamada=" + s.getClaveLlamada());

        // ===UPDATE DIRECTO a la tabla de SolicitudTipoCambio (en vez de save) ===
        int rows = solicitudTipoCambioRepository.updateCamposPacto(
                s.getId(),
                s.getContacto(),
                s.getClaveLlamada(),
                s.getTipoCambio() // 'int' en tu entidad
        );
        System.out.println("[PACTO][SOL][UPDATE] filas afectadas=" + rows);

        // (Opcional) Relee desde BD para confirmar
        SolicitudTipoCambio check = solicitudTipoCambioRepository.readForCheck(s.getId()).orElse(null);
        System.out.println("[PACTO][DB][SOL] id=" + s.getId()
                + ", tipoCambio=" + (check != null ? check.getTipoCambio() : "N/A")
                + ", contacto=" + (check != null ? check.getContacto() : "N/A")
                + ", claveLlamada=" + (check != null ? check.getClaveLlamada() : "N/A"));

                        // Enviar correo (puedes extraer más datos si lo necesitas)
        String asunto = "Confirmación de acuerdo de tipo de cambio";
        String cuerpo = String.format(
                "Se ha confirmado el acuerdo de tipo de cambio para la instrucción con folio %s.\n\n" +
                        "Contacto: %s\nDivisa de pago: %s\nDivisa de compra: %s\nTipo de cambio: %s\nClave de llamada: %s\n",
                s.getOperacion().getInstruccion().getFolio(), // suponiendo esta relación
                s.getContacto(),
                s.getDivisaPago(),
                s.getDivisaCompra(),
                s.getTipoCambio(),
                s.getClaveLlamada());

        emailService.sendEmail("gvazquezh@mcllent.com", asunto, cuerpo); 
    }

    public List<SolicitudTipoCambioDto> obtenerSolicitudes(String userEmail, SolicitudTipoCambioDto filtro, LocalDate desde, LocalDate hasta, Pageable pageable) {
        SolicitudTipoCambio probe = new SolicitudTipoCambio();

        if(filtro.getDivisaCompra() != null){
            Divisa divisa = new Divisa();
            divisa.setCve(filtro.getDivisaCompra());
            probe.setDivisaCompra(divisa);
        }

        if(filtro.getDivisaPago() != null){
            Divisa divisaP = new Divisa();
            divisaP.setCve(filtro.getDivisaPago());
            probe.setDivisaPago(divisaP);
        }

        return solicitudTipoCambioMapper.toDto(solicitudTipoCambioRepository.findAll(filtroTipoCambio(probe, desde, hasta
                , empleadoService.buscarEmpleado(userEmail).getRegiones()
                        .stream().map(r -> r.getCve())
                        .collect(Collectors.toSet())),pageable).getContent());
    }
    private static Specification<SolicitudTipoCambio> filtroTipoCambio(SolicitudTipoCambio filtro, LocalDate desde, LocalDate hasta, Set<String> regiones){
        return (root, query, cb) -> {

            List<Predicate> predicates = new ArrayList<>();

            if (desde != null && hasta != null) {
                predicates.add(cb.between(root.get("fechaSolicitud"), desde.atStartOfDay(), hasta.atTime(23,59)));
            }

            if(!regiones.isEmpty()) {
                Join<SolicitudTipoCambio, OperacionMonetaria> joinOperacion = root.join("operacion", JoinType.LEFT);
                Join<OperacionMonetaria, InstruccionMonetaria> joinInstruccionMonetaria = joinOperacion.join("instruccion", JoinType.LEFT);
                Join<InstruccionMonetaria, Instruccion> joinInstruccion = joinInstruccionMonetaria.join("instruccion", JoinType.LEFT);
                Join<CuentaAbono, Fideicomiso> joinFideicomiso = joinInstruccion.join("fideicomiso", JoinType.LEFT);
                Join<Fideicomiso, Region> regionJoin = joinFideicomiso.join("region", JoinType.LEFT);
                predicates.add(regionJoin.get("cve").in(regiones));
            }

            if(filtro.getDivisaCompra() != null){
                Join<SolicitudTipoCambio, Divisa> joinDivisa = root.join("divisaCompra");
                predicates.add(cb.equal(joinDivisa.get("cve"), filtro.getDivisaCompra().getCve()));
            }

            if(filtro.getDivisaPago() != null){
                Join<SolicitudTipoCambio, Divisa> joinDivisaPago = root.join("divisaPago");
                predicates.add(cb.equal(joinDivisaPago.get("cve"), filtro.getDivisaPago().getCve()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

}