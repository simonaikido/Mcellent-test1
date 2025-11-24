package com.bim.seif.services;

import com.bim.seif.models.Instruccion;
import com.bim.seif.models.Propiedad;
import com.bim.seif.models.TipoEvento;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class NotificacionService {

    private final EmailService emailService;

    private static final ZoneId MX = ZoneId.of("America/Mexico_City");
    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // Formatea a "yyyy-MM-dd" aceptando varios tipos de fecha
    private static String fmtYMD(Object value) {
        if (value == null) return "";
        if (value instanceof LocalDate d)         return d.format(YMD);
        if (value instanceof LocalDateTime ldt)   return ldt.toLocalDate().format(YMD);
        if (value instanceof Instant i)           return YMD.withZone(MX).format(i);
        if (value instanceof java.util.Date d)    return YMD.withZone(MX).format(d.toInstant());
        return value.toString(); // último recurso (no lanzar excepción)
    }

    @AfterReturning(
        pointcut =
            "execution(* com.bim.seif.repositories.InstruccionRepository.save(..)) || " +
            "execution(* com.bim.seif.repositories.InstruccionRepository.saveAndFlush(..))",
        returning = "resultado"
    )
    public void notificar(Object resultado) {

        if (!(resultado instanceof Instruccion instruccion)) {
            return; // solo nos interesan notificaciones de Instruccion
        }

        try {
            Map<Propiedad, String> variables = new HashMap<>();
            if (instruccion.getFideicomiso() != null) {
                variables.put(Propiedad.fideicomiso_folio, instruccion.getFideicomiso().getFolio());
                variables.put(Propiedad.fideicomiso_alias, instruccion.getFideicomiso().getAlias());
            }

            variables.put(Propiedad.instruccion_folio, instruccion.getFolio());

            // Por defecto usamos la "fecha de recepción" como la fecha de alta
            variables.put(Propiedad.cuenta_numero.instruccion_fecha_recepcion,
                          fmtYMD(instruccion.getFechaAlta()));

            // Determina el tipo de evento según las fechas presentes:
            // prioridad: rechazo -> ejecución/atención -> recepción
            TipoEvento evento = null;
            if (instruccion.getFechaRechazo() != null) {
                evento = TipoEvento.instruccion_rechazo;
            } else if (instruccion.getFechaAtencion() != null) {
                evento = TipoEvento.instruccion_ejecucion;
                variables.put(Propiedad.cuenta_numero.instruccion_fecha_recepcion,
                              fmtYMD(instruccion.getFechaAtencion()));
            } else if (instruccion.getTipo() != null) {
                evento = TipoEvento.instruccion_recepcion;
            }

            if (evento != null && instruccion.getFideicomiso() != null) {
                emailService.enviarCorreoAsociados(
                    instruccion.getFideicomiso().getFolio(),
                    evento, // <-- usar el evento calculado, no forzar RE
                    variables
                );
            }
        } catch (Exception e) {
            // Nunca tumbar el flujo de negocio por una notificación
            log.error("Error al armar/enviar notificación para instrucción {}: {}",
                      (resultado instanceof Instruccion i ? i.getFolio() : "N/A"),
                      e.getMessage(), e);
        }
    }
}