package com.bim.seif.services;

import com.bim.seif.models.PasswordResetToken;
import com.bim.seif.models.Propiedad;
import com.bim.seif.models.TipoEvento;
import com.bim.seif.models.dto.EmpleadoDto;
import com.bim.seif.repositories.PasswordResetTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.naming.InvalidNameException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordResetService {

    private final PasswordResetTokenRepository tokenRepo;
    private final EmpleadoService empleadoService;
    private final EmailService emailService;

    /** Zona oficial MX – solo para LOGS (no persistimos en MX) */
    private static final ZoneId MX = ZoneId.of("America/Mexico_City");

    /** TTL del token en minutos (configurable) */
    @Value("${password-reset.ttl-minutes}")
    private long ttlMinutes;

    /** Base del front, p.ej. http://localhost:4200 */
    @Value("${frontend.base-url}")
    private String frontBaseUrl;

    /** Ruta del componente de cambio de contraseña, p.ej. /seif/cambio-contrasena */
    @Value("${frontend.reset-path}")
    private String frontResetPath;

    /**
     * Genera token, persiste SIEMPRE en UTC y envía correo con la liga clickeable.
     */
    public void enviarLigaReset(String uid, String ip, String ua) throws InvalidNameException {
        // 1) valida usuario (lanza si no existe)
        empleadoService.loadUserByUsername(uid);

        // 2) token seguro
        String token = generarTokenSeguro();

        // 3) instantes en UTC (persistimos SOLO UTC)
        final long ttl = effectiveTtlMinutes(ttlMinutes);
        final Instant nowUtc = Instant.now();
        Instant expUtc = nowUtc.plus(ttl, ChronoUnit.MINUTES);

        // failsafe si ttl viene inválido
        if (!expUtc.isAfter(nowUtc)) {
            log.warn("TTL inválido ({} min). Forzando 30 min de vigencia.", ttlMinutes);
            expUtc = nowUtc.plus(30, ChronoUnit.MINUTES);
        }

        // 4) persistimos
        PasswordResetToken prt = PasswordResetToken.builder()
                .uid(uid)
                .token(token)
                .createdAt(nowUtc)   // UTC
                .expiresAt(expUtc)   // UTC
                .used(false)
                .createdByIp(ip)
                .createdByUa(ua)
                .build();
        tokenRepo.save(prt);

        // 5) liga segura
        String tokenUrl = frontBaseUrl + ensureLeadingSlash(frontResetPath) + "?token=" +
                URLEncoder.encode(token, StandardCharsets.UTF_8);

        // 6) correo (link clickeable)
        emailService.enviarCorreo(
                uid,
                TipoEvento.reestablecer_contrasena_PI,
                Map.of(
                        Propiedad.empleado_email, uid,
                        Propiedad.url, tokenUrl
                )
        );

        // 7) LOG resumido (sin stacktraces)
        log.info("Reset enviado -> uid={}, id={}, created_utc={}, expires_utc={}, created_mx={}, expires_mx={}, ip={}, ua={}",
                uid, prt.getId(),
                nowUtc, expUtc,
                nowUtc.atZone(MX), expUtc.atZone(MX),
                ip, ua);
    }

    /**
     * Verifica que el token exista, no esté usado y no esté vencido.
     * Loguea de forma breve y retorna el uid si es válido.
     */
    public String validarToken(String token) {
        PasswordResetToken prt = tokenRepo.findByTokenCS(token) // búsqueda CASE-SENSITIVE
                .orElseThrow(() -> {
                    log.warn("validarToken -> token inexistente/alterado");
                    return new IllegalArgumentException("token inválido");
                });

        Instant nowUtc = Instant.now();

        if (prt.isUsed()) {
            log.warn("validarToken -> token ya usado (id={}, uid={})", prt.getId(), prt.getUid());
            throw new IllegalStateException("token usado");
        }
        if (prt.getExpiresAt().isBefore(nowUtc)) {
            log.warn("validarToken -> token vencido (id={}, uid={}, exp_utc={}, now_utc={})",
                    prt.getId(), prt.getUid(), prt.getExpiresAt(), nowUtc);
            throw new IllegalStateException("token vencido");
        }

        log.info("validarToken OK -> id={}, uid={}, exp_utc={}, now_utc={}, exp_mx={}",
                prt.getId(), prt.getUid(), prt.getExpiresAt(), nowUtc, prt.getExpiresAt().atZone(MX));
        return prt.getUid();
    }

    /**
     * Aplica el cambio de password y marca el token como usado (UTC).
     * Loguea de forma breve (sin stacktraces).
     */
    public void consumirTokenYCambiarPassword(String token, String newPasswordHash) {
        PasswordResetToken prt = tokenRepo.findByTokenCS(token)
                .orElseThrow(() -> {
                    log.warn("consumirToken -> token inexistente/alterado");
                    return new IllegalArgumentException("token inválido");
                });

        Instant nowUtc = Instant.now();

        if (prt.isUsed()) {
            log.warn("consumirToken -> token ya usado (id={}, uid={})", prt.getId(), prt.getUid());
            throw new IllegalStateException("token ya usado");
        }
        if (prt.getExpiresAt().isBefore(nowUtc)) {
            log.warn("consumirToken -> token vencido (id={}, uid={}, exp_utc={}, now_utc={})",
                    prt.getId(), prt.getUid(), prt.getExpiresAt(), nowUtc);
            throw new IllegalStateException("token vencido");
        }

        // Cambia la contraseña (el servicio aplica la política/encoder)
        EmpleadoDto cambios = new EmpleadoDto();
        cambios.setPassword(newPasswordHash);
        empleadoService.actualizarEmpleado(prt.getUid(), cambios);

        // Marcar como usado (UTC)
        prt.setUsed(true);
        prt.setUsedAt(nowUtc);
        tokenRepo.save(prt);

        log.info("consumirToken OK -> id={}, uid={}, used_at_utc={}", prt.getId(), prt.getUid(), nowUtc);
    }

    /* Helpers */

    private static final SecureRandom SR = new SecureRandom();

    private String generarTokenSeguro() {
        byte[] rnd = new byte[32];
        SR.nextBytes(rnd);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(rnd);
    }

    /** Normaliza TTL: si viene ≤0 o > 24h, usa 30 min. */
    private long effectiveTtlMinutes(long raw) {
        if (raw <= 0 || raw > (24 * 60)) return 30L;
        return raw;
    }

    /** Asegura que la ruta del front tenga "/" inicial. */
    private String ensureLeadingSlash(String path) {
        if (path == null || path.isEmpty()) return "/";
        return path.startsWith("/") ? path : "/" + path;
    }
}