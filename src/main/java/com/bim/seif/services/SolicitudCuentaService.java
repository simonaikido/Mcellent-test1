package com.bim.seif.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.transaction.Transactional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.HtmlUtils;

import com.bim.seif.models.CuentaAbono;
import com.bim.seif.models.Divisa;
import com.bim.seif.models.Instruccion;
import com.bim.seif.models.Propiedad;
import com.bim.seif.models.SolicitudCuenta;
import com.bim.seif.models.TipoEvento;
import com.bim.seif.models.dto.SolicitudCuentaDto;
import com.bim.seif.models.dto.SolicitudCuentaResponseDto;
import com.bim.seif.models.dto.SolicitudCuentaResumenDto;
import com.bim.seif.repositories.CuentaRepository;
import com.bim.seif.repositories.DivisaRepository;
import com.bim.seif.repositories.InstruccionRepository;
import com.bim.seif.repositories.SolicitudCuentaRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SolicitudCuentaService {

    @Autowired
    private SolicitudCuentaRepository solicitudCuentaRepository;

    @Autowired
    private InstruccionRepository instruccionRepository;

    @Autowired
    private DivisaRepository divisaRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private CuentaRepository cuentaAbonoRepository;

    @Value("${bim.portal.ext.url}")
    private String linkPortal;

    @Transactional
    public SolicitudCuentaResponseDto crearSolicitudCuenta(
            String instruccionFolio,
            SolicitudCuentaDto solicitudCuentaDTO) {

        log.debug("Creando solicitud de cuenta: {}", solicitudCuentaDTO);

        if (cuentaAbonoRepository.findById(solicitudCuentaDTO.getCuenta()).isPresent()) {
            throw new RuntimeException("Ya existe el número de cuenta");
        }

        Instruccion instruccion = instruccionRepository.findById(instruccionFolio)
                .orElseThrow(() -> new RuntimeException(
                        "No se encontró la instrucción con el folio: " + instruccionFolio));

        Divisa divisa = divisaRepository.findById(solicitudCuentaDTO.getDivisaCve())
                .orElseThrow(() -> new RuntimeException(
                        "No se encontró la divisa con la clave: " + solicitudCuentaDTO.getDivisaCve()));

        // 1) Guardamos la solicitud
        SolicitudCuenta nuevaSolicitud = new SolicitudCuenta();
        nuevaSolicitud.setCuenta(solicitudCuentaDTO.getCuenta());
        nuevaSolicitud.setBanco(solicitudCuentaDTO.getBanco());
        nuevaSolicitud.setDivisa_cve(divisa);
        nuevaSolicitud.setBeneficiario(solicitudCuentaDTO.getBeneficiario());
        nuevaSolicitud.setRfc(solicitudCuentaDTO.getRfc());
        nuevaSolicitud.setDireccion(solicitudCuentaDTO.getDireccion());
        nuevaSolicitud.setRutaEdoCta(solicitudCuentaDTO.getRutaEdoCta());
        nuevaSolicitud.setFechaSolicitud(LocalDateTime.now());
        nuevaSolicitud.setInstruccion(instruccion);
        nuevaSolicitud.setFideicomiso_folio(instruccion.getFideicomiso());

        SolicitudCuenta solicitudGuardada = solicitudCuentaRepository.save(nuevaSolicitud);

        // 2) Insertamos también en cuenta_abono como "pendiente"
        CuentaAbono cuenta = cuentaAbonoRepository.findById(solicitudCuentaDTO.getCuenta())
                .orElseGet(CuentaAbono::new);

        cuenta.setCuenta(solicitudCuentaDTO.getCuenta());
        cuenta.setBanco(solicitudCuentaDTO.getBanco());
        cuenta.setBeneficiario(solicitudCuentaDTO.getBeneficiario());
        cuenta.setDireccion(solicitudCuentaDTO.getDireccion());
        cuenta.setRfc(solicitudCuentaDTO.getRfc());
        cuenta.setDivisa(divisa);
        cuenta.setFideicomiso(instruccion.getFideicomiso());
        cuenta.setSolicitudCuenta(true); // viene de solicitud

        cuentaAbonoRepository.save(cuenta);

        // 3) Notificación por correo usando el nuevo Evento
        if (solicitudCuentaDTO.isSolicitarEstadoCuenta()) {
            String correoDestino = instruccion.getClienteCarga(); // destinatario
            if (correoDestino != null && !correoDestino.isEmpty()) {

                try {
                    emailService.enviarCorreo(
                            correoDestino,
                            TipoEvento.cuenta_solicitud,
                            Map.of(Propiedad.url, linkPortal));
                    log.info("Correo de 'cuenta_solicitud' enviado a {}", correoDestino);
                } catch (Exception e) {
                    log.error("Error al enviar correo de 'cuenta_solicitud' a {}: {}", correoDestino, e.getMessage(),
                            e);
                    // Decide si quieres fallar la transacción o solo registrar el error
                    // throw e;
                }
            } else {
                log.warn("No se envió correo: 'clienteCarga' no definido para la instrucción {}", instruccionFolio);
            }
        }

        log.debug("Solicitud de cuenta creada: {}", solicitudGuardada);
        return mapToDto(solicitudGuardada);
    }

    public List<SolicitudCuentaResponseDto> getSolicitudesPorFideicomiso(String folioFideicomiso) {
        log.debug("Buscando solicitudes por fideicomiso: {}", folioFideicomiso);
        List<SolicitudCuenta> solicitudes = solicitudCuentaRepository.findByFideicomisoFolio(folioFideicomiso);
        return solicitudes.stream().map(this::mapToDto).collect(Collectors.toList());
    }

    // Nuevo metodo de servicio para buscar solicitudes por una lista de CVEs de
    // región
    // agrega validacion de fecha
    public List<SolicitudCuentaResponseDto> getSolicitudesPorRegionCve(List<String> cveRegiones) {
        log.debug("Buscando solicitudes por CVE de región: {}", cveRegiones);
        List<SolicitudCuenta> solicitudes = solicitudCuentaRepository.findByFideicomisoRegionCveIn(cveRegiones);

        return solicitudes.stream()
                .filter(solicitud -> solicitud.getInstruccion() == null || solicitud.getFechaFinalizacion() == null)
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private SolicitudCuentaResponseDto mapToDto(SolicitudCuenta entity) {

        log.debug("Mapeando entidad a DTO: {}", entity);
        SolicitudCuentaResponseDto dto = new SolicitudCuentaResponseDto();

        dto.setId(entity.getId());
        dto.setCuenta(entity.getCuenta());
        dto.setBanco(HtmlUtils.htmlEscape(entity.getBanco()));
        dto.setDivisaCve(HtmlUtils.htmlEscape(entity.getDivisa_cve().getCve()));
        dto.setBeneficiario(HtmlUtils.htmlEscape(entity.getBeneficiario()));
        dto.setRfc(HtmlUtils.htmlEscape(entity.getRfc()));
        dto.setDireccion(HtmlUtils.htmlEscape(entity.getDireccion()));
        dto.setRutaEdoCta(HtmlUtils.htmlEscape(entity.getRutaEdoCta()));
        dto.setRutaInstruccion(HtmlUtils.htmlEscape(entity.getInstruccion().getRutaArchivo()));
        dto.setFechaSolicitud(entity.getFechaSolicitud());
        dto.setFechaCarga(entity.getFechaCarga());
        dto.setFechaFinalizacion(entity.getFechaFinalizacion());

        dto.setFolioInstruccion(HtmlUtils.htmlEscape(entity.getInstruccion().getFolio()));

        if (entity.getFideicomiso_folio() != null) {
            dto.setFolioFideicomiso(HtmlUtils.htmlEscape(entity.getFideicomiso_folio().getFolio()));
        }

        return dto;
    }

    @Transactional
    public void rechazarSolicitud(Long idSolicitud) {

        if (idSolicitud == null || idSolicitud <= 0) {
            throw new IllegalArgumentException("ID de solicitud inválido");
        }

        SolicitudCuenta solicitud = solicitudCuentaRepository.findById(idSolicitud)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada con id: " + idSolicitud));

        solicitud.setFechaFinalizacion(LocalDateTime.now());

        Instruccion instruccion = solicitud.getInstruccion();
        if (instruccion != null) {
            instruccion.setFechaRechazo(LocalDateTime.now());
            instruccionRepository.save(instruccion);

            // Obtener correo desde Instruccion.clienteCarga
            String correoDestino = instruccion.getClienteCarga();
            if (correoDestino != null && !correoDestino.isEmpty()) {
                emailService.sendEmail(
                        correoDestino,
                        "Solicitud de cuenta rechazada",
                        "Estimado usuario,\n\nLamentamos informarte que la solicitud de alta de cuenta ha sido rechazada.\n\nSaludos,\nEquipo BIM");
            }
        }

        solicitudCuentaRepository.save(solicitud);
    }

    @Transactional
    public void aceptadaSolicitud(Long idSolicitud, SolicitudCuentaResumenDto payload) {

        if (idSolicitud == null || idSolicitud <= 0) {
            throw new IllegalArgumentException("ID de solicitud inválido");
        }

        SolicitudCuenta solicitud = solicitudCuentaRepository.findById(idSolicitud)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada con id: " + idSolicitud));
        solicitud.setContacto(HtmlUtils.htmlEscape(payload.getContacto()));
        solicitud.setCuenta(Long.parseLong(payload.getCuenta()));
        solicitud.setBanco(HtmlUtils.htmlEscape(payload.getBanco()));
        solicitud.setBeneficiario(HtmlUtils.htmlEscape(payload.getBeneficiario()));
        solicitud.setRfc(HtmlUtils.htmlEscape(payload.getRfc()));
        solicitud.setDireccion(HtmlUtils.htmlEscape(payload.getDireccion()));
        solicitud.setFechaFinalizacion(LocalDateTime.now());
        // update de cuanta abono para cuenta
        Long cuenta = solicitud.getCuenta();
        if (cuenta == null) {
            throw new IllegalArgumentException("La solicitud no tiene número de cuenta asociado.");
        }
        CuentaAbono cuentaAbono = cuentaAbonoRepository.findById(cuenta)
                .orElseThrow(
                        () -> new javax.persistence.EntityNotFoundException("CuentaAbono no encontrada: " + cuenta));
        cuentaAbono.setCuenta(Long.parseLong(payload.getCuenta()));
        cuentaAbono.setBanco(HtmlUtils.htmlEscape(payload.getBanco()));
        cuentaAbono.setBeneficiario(HtmlUtils.htmlEscape(payload.getBeneficiario()));
        cuentaAbono.setRfc(HtmlUtils.htmlEscape(payload.getRfc()));
        cuentaAbono.setDireccion(HtmlUtils.htmlEscape(payload.getDireccion()));
        cuentaAbono.setSolicitudCuenta(Boolean.FALSE);// actualizamos la cuenta parq eu ya no sea una solicitud
        cuentaAbono.setRutaArchivo(HtmlUtils.htmlEscape(solicitud.getRutaEdoCta()));
        cuentaAbonoRepository.save(cuentaAbono);

        Instruccion instruccion = solicitud.getInstruccion();
        if (instruccion != null) {
            // Obtener correo desde Instruccion.clienteCarga
            String correoDestino = instruccion.getClienteCarga();
            if (correoDestino != null && !correoDestino.isEmpty()) {
                emailService.sendEmail(
                        correoDestino,
                        "Solicitud de cuenta Aprobada",
                        "Estimado usuario,\n\nNos es un gusto informarte que la solicitud de alta de cuenta ha sido aceptada.\n\nSaludos,\nEquipo BIM");
            }
        }
        solicitudCuentaRepository.save(solicitud);
    }

    @Transactional
    public void correcionSolicitud(Long idSolicitud) {

        if (idSolicitud == null || idSolicitud <= 0) {
            throw new IllegalArgumentException("ID de solicitud inválido");
        }

        SolicitudCuenta solicitud = solicitudCuentaRepository.findById(idSolicitud)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Solicitud no encontrada con id: " + idSolicitud));

        Instruccion instruccion = solicitud.getInstruccion();
        if (instruccion != null) {
            // Obtener correo desde Instruccion.clienteCarga
            String correoDestino = instruccion.getClienteCarga();
            if (correoDestino != null && !correoDestino.isEmpty()) {
                correoDestino = correoDestino.trim();
                emailService.sendEmail(
                        correoDestino,
                        "Solicitud de Correcion de cuenta ",
                        "Estimado usuario,\n\nNo es un gusto informarte que la solicitud de alta de cuenta necesita una correccion contactese con nosotros.\n\nSaludos,\nEquipo BIM");
            }
        }
    }

}