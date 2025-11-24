package com.bim.seif.services;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import com.bim.seif.models.EmpleadoAudit;
import com.bim.seif.models.dto.EmpleadoDto;
import com.bim.seif.models.dto.FiltroUsuariosInternosDto;
import com.bim.seif.repositories.EmpleadoAuditRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditoriaService {

  private final EmpleadoAuditRepository repo;
  private final ObjectMapper mapper;

  public void registrarAltaEmpleado(EmpleadoDto dto, String actor, String ip, String ua) {
    try {
		EmpleadoDto safeDto = new EmpleadoDto();
	    safeDto.setUid(HtmlUtils.htmlEscape(dto.getUid()));
	    safeDto.setEmail(HtmlUtils.htmlEscape(dto.getEmail()));
	    safeDto.setNombre(HtmlUtils.htmlEscape(dto.getNombre()));
	    safeDto.setSn(HtmlUtils.htmlEscape(dto.getSn()));
	    safeDto.setArea(HtmlUtils.htmlEscape(dto.getArea()));
	    safeDto.setAuditNote(HtmlUtils.htmlEscape(dto.getAuditNote()));
	    safeDto.setActivo(dto.getActivo());
	    safeDto.setRol(dto.getRol());
	    safeDto.setRegiones(dto.getRegiones());
	    safeDto.setNumeroEmpleado(HtmlUtils.htmlEscape(dto.getNumeroEmpleado()));
	    safeDto.setCelular(HtmlUtils.htmlEscape(dto.getCelular()));
	    safeDto.setExtension(HtmlUtils.htmlEscape(dto.getExtension()));
      
	    EmpleadoAudit a = new EmpleadoAudit();
	    a.setUid(HtmlUtils.htmlEscape(dto.getUid()));
        a.setAccion("CREATED");
        a.setActor(actor != null ? HtmlUtils.htmlEscape(actor) : "system");
        a.setIp(HtmlUtils.htmlEscape(ip));
        a.setUserAgent(HtmlUtils.htmlEscape(ua));
        a.setPayloadBefore(null);
        a.setPayloadAfter(mapper.writeValueAsString(safeDto));
        
        repo.save(a);
    } catch (Exception ignore) {
      log.error("Error registrando alta de empleado: {}", ignore.getMessage());
      // Log si quieres, pero no tires la transacción por la auditoría.
    }
  }

  public void registrarModificacionEmpleado(
      String uid,
      EmpleadoDto before,
      EmpleadoDto after,
      String actor,
      String ip,
      String ua,
      String comentario) {

    log.info("Registrando modificacion de empleado: {}", uid);
    try {
      EmpleadoAudit a = new EmpleadoAudit();
      a.setUid(uid);
      a.setAccion("UPDATED");
      a.setActor(actor != null ? actor : "system");
      a.setIp(ip);
      a.setUserAgent(ua);

      EmpleadoDto safeBefore = before;
      EmpleadoDto safeAfter = after;

      a.setPayloadBefore(mapper.writeValueAsString(safeBefore));
      a.setPayloadAfter(mapper.writeValueAsString(safeAfter));
      a.setComentario(comentario);

      repo.save(a);
    } catch (Exception e) {
    }
  }

  public void registrarCambioActivo(String uid, boolean activo, String actor, String ip, String ua) {
    try {
      EmpleadoAudit a = new EmpleadoAudit();
      a.setUid(uid);
      a.setAccion(activo ? "ENABLE_USER" : "DISABLE_USER");
      a.setActor(actor != null ? actor : "system");
      a.setIp(ip);
      a.setUserAgent(ua);
      a.setPayloadBefore(null);
      a.setPayloadAfter("{\"activo\": " + activo + "}");
      repo.save(a);
    } catch (Exception e) {
      log.error("Fallo al registrar auditoria de cambio de activo: {}", e.getMessage());
    }
  }

  public void registrarEliminacionUsuario(String uid, String actor, String ip, String ua) {
    try {
      EmpleadoAudit a = new EmpleadoAudit();
      a.setUid(uid);
      a.setAccion("DELETE_USER");
      a.setActor(actor != null ? actor : "system");
      a.setIp(ip);
      a.setUserAgent(ua);
      a.setPayloadBefore("{\"uid\": \"" + uid + "\"}");
      a.setPayloadAfter(null);
      repo.save(a);
    } catch (Exception e) {
      log.error("Fallo al registrar auditoria de eliminacion de usuario: {}", e.getMessage());
    }
  }

  // ===== Registrar consulta del listado de usuarios internos =====
  public void registrarConsultaListadoUsuarios(
      String actor,
      String ip,
      String userAgent,
      FiltroUsuariosInternosDto filtro,
      LocalDate desde,
      LocalDate hasta,
      FiltroUsuariosInternosDto.CampoFecha campoFecha,
      int total) {
    try {
      EmpleadoAudit audit = new EmpleadoAudit();
      audit.setUid(actor);
      audit.setAccion("CONSULTA_LISTADO_USUARIOS_INTERNOS");
      audit.setActor(actor);
      audit.setIp(ip);
      audit.setUserAgent(userAgent);
      audit.setComentario("Consulta de reporte de usuarios internos — total resultados: " + total);

      // Serializamos un resumen de los filtros aplicados
      String payload = """
          {
            "filtro": {
              "celular": "%s",
              "extension": "%s",
              "estatus": "%s",
              "usuarioAlta": "%s",
              "usuarioBaja": "%s",
              "auditoriaBaja": "%s"
            },
            "rango": {
              "desde": "%s",
              "hasta": "%s",
              "campoFecha": "%s"
            },
            "total": %d
          }
          """.formatted(
            filtro != null ? HtmlUtils.htmlEscape(filtro.getCelular()) : "",
		    filtro != null ? HtmlUtils.htmlEscape(filtro.getExtension()) : "",
		    filtro != null ? HtmlUtils.htmlEscape(filtro.getEstatus()) : "",
		    filtro != null ? HtmlUtils.htmlEscape(filtro.getUsuarioAlta()) : "",
		    filtro != null ? HtmlUtils.htmlEscape(filtro.getUsuarioBaja()) : "",
		    filtro != null ? HtmlUtils.htmlEscape(filtro.getAuditoriaBaja()) : "",
		    HtmlUtils.htmlEscape(String.valueOf(desde)),
		    HtmlUtils.htmlEscape(String.valueOf(hasta)),
		    HtmlUtils.htmlEscape(String.valueOf(campoFecha)),
		    total);

      audit.setPayloadAfter(payload);
      audit.setCreatedAt(java.time.OffsetDateTime.now());

      // Guarda en base de datos
      repo.save(audit);
    } catch (Exception e) {
      // No debe romper el flujo principal si falla la auditoría
      System.err.println("No se pudo registrar auditoría de reporte: " + e.getMessage());
    }
  }

  public void registrarOtpSolicitado(String uid, String ip, String userAgent, String comentario) {
    EmpleadoAudit audit = new EmpleadoAudit();
    audit.setUid(safe(uid, "desconocido"));
    audit.setAccion("OTP_REQUESTED");
    audit.setActor(safe(uid, "anon"));
    audit.setIp(ip);
    audit.setUserAgent(userAgent);
    audit.setComentario(comentario);
    audit.setCreatedAt(OffsetDateTime.now());
    repo.save(audit);
  }

  public void registrarLoginOk(String uid, String ip, String userAgent, EmpleadoDto snapshot, String comentario) {
    log.info("Registrando login exitoso para usuario: {}", uid);
    EmpleadoAudit audit = new EmpleadoAudit();
    audit.setUid(safe(uid, "desconocido"));
    audit.setAccion("LOGIN_OK");
    audit.setActor(safe(uid, "anon"));
    audit.setIp(ip);
    audit.setUserAgent(userAgent);
    audit.setComentario(comentario);
    audit.setCreatedAt(OffsetDateTime.now());

    // payloadAfter con snapshot resumido + authTimestamp
    Map<String, Object> payload = new HashMap<>();
    payload.put("authTimestamp", OffsetDateTime.now().toString());
    payload.put("uid", safe(snapshot != null ? snapshot.getUid() : null, uid));
    payload.put("email", snapshot != null ? snapshot.getEmail() : null);
    payload.put("nombre", snapshot != null ? snapshot.getNombre() : null);
    payload.put("rol", snapshot != null ? snapshot.getRol() : null);
    payload.put("regiones", snapshot != null ? snapshot.getRegiones() : null);
    payload.put("activo", snapshot != null ? snapshot.getActivo() : null);
    payload.put("authTimestamp", OffsetDateTime.now().toString());
    payload.put("metodo", "OTP");

    ObjectMapper om = new ObjectMapper();
    try {
      audit.setPayloadAfter(om.writeValueAsString(payload));
    } catch (Exception ignore) {
    }
    repo.save(audit);
  }

  public void registrarLoginFail(String uid, String ip, String userAgent, String motivo) {
    log.info("Registrando login fallido para usuario: {}", uid);
    EmpleadoAudit audit = new EmpleadoAudit();
    audit.setUid(safe(uid, "desconocido"));
    audit.setAccion("LOGIN_FAIL");
    audit.setActor(safe(uid, "anon"));
    audit.setIp(ip);
    audit.setUserAgent(userAgent);
    audit.setComentario(motivo);
    audit.setCreatedAt(OffsetDateTime.now());
    repo.save(audit);
  }

  private static String safe(String v, String def) {
    return (v == null || v.isBlank()) ? def : v;
  }
}