package com.bim.seif.controllers;

import com.bim.seif.exceptions.FirstLoginRequiredException;
import com.bim.seif.exceptions.UsuarioBloqueadoException;
import com.bim.seif.models.dto.AuthRequest;
import com.bim.seif.models.dto.EmpleadoDto;
import com.bim.seif.security.AccessTokenFactory;
import com.bim.seif.security.RefreshTokenService;
import com.bim.seif.services.AuditoriaService;
import com.bim.seif.services.AuthService;
import com.bim.seif.services.EmpleadoService;

import io.micrometer.core.annotation.Timed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

import javax.naming.InvalidNameException;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.HtmlUtils;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuditoriaService auditoriaService;
    private final EmpleadoService empleadoService;

    private final AccessTokenFactory accessTokenFactory;
    private final RefreshTokenService refreshTokenService;
    private final JwtDecoder jwtDecoder;

    @Value("${security.oauth2.access-token.ttl-minutes}")
    private int accessTtlMinutes;

    @Timed(value = "auth.otp.latency", description = "Tiempo de respuesta del endpoint /otp", histogram = true)
    @PostMapping("/otp")
    public ResponseEntity<res> solicitarCodigoAcceso(
            @RequestBody AuthRequest request,
            HttpServletRequest http) throws Exception {

        String uid = resolveUid(request);
        String ip = clientIp(http);
        String ua = http.getHeader("User-Agent");
        ua = HtmlUtils.htmlEscape(ua);
        
        try {
            authService.enviarCodigoAutenticacion(request);
            auditoriaService.registrarOtpSolicitado(uid, ip, ua, "OTP enviado");
            return ResponseEntity.ok(new res("OK"));

        } catch (FirstLoginRequiredException ex) {
            // No enviamos OTP: forzar cambio de contraseña
            auditoriaService.registrarOtpSolicitado(uid, ip, ua, "REQUIERE_CAMBIO_PASSWORD");
            // 428 Precondition Required (contracto claro para el front)
            return ResponseEntity.status(428).body(new res("REQUIERE_CAMBIO_PASSWORD"));

        } catch (UsuarioBloqueadoException ex) {
            auditoriaService.registrarOtpSolicitado(uid, ip, ua, "Usuario bloqueado");
            return ResponseEntity.status(423).body(new res("USUARIO_BLOQUEADO")); // 423 Locked (opcional)

        } catch (BadCredentialsException ex) {
            auditoriaService.registrarOtpSolicitado(uid, ip, ua, "Credenciales inválidas");
            return ResponseEntity.status(401).body(new res("CREDENCIALES_INVALIDAS"));
        }
    }

    class res {
        private String code;

        public res(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }
    }

    @PostMapping("/credenciales")
    public ResponseEntity<?> autenticar(
            @RequestBody AuthRequest request,
            HttpServletRequest http) throws UsernameNotFoundException, InvalidNameException {

        String uid = resolveUid(request);
        String ip = clientIp(http);
        String ua = http.getHeader("User-Agent");
        ua = HtmlUtils.htmlEscape(ua);

        try {
            ResponseEntity<?> resp = authService.autenticar(request);

            if (resp.getStatusCode().is2xxSuccessful()) {
                EmpleadoDto snapshot;
                try {
                    snapshot = empleadoService.loadUserByUsername(uid);
                } catch (Exception e) {
                    snapshot = new EmpleadoDto();
                    snapshot.setUid(uid);
                }
                auditoriaService.registrarLoginOk(uid, ip, ua, snapshot, "OTP confirmado / login exitoso");
            } else {
                auditoriaService.registrarLoginFail(uid, ip, ua,
                        "Login no exitoso: HTTP " + resp.getStatusCodeValue());
            }

            return resp;

        } catch (UsuarioBloqueadoException ex) {
            auditoriaService.registrarLoginFail(uid, ip, ua, "Usuario bloqueado por OTP");
            // 423 Locked comunica claramente el bloqueo; usa 403 si prefieres.
            return ResponseEntity.status(423).body(Map.of("mensaje", "Usuario bloqueado por OTP"));
        }
    }

    private String resolveUid(AuthRequest r) {
        if (r == null)
            return "desconocido";
        
        String uid = r.getUsuario();
        if (uid == null || uid.isBlank())
            return "desconocido";
        
        uid = uid.replaceAll("[^a-zA-Z0-9_-]", "");
        
        return uid;
    }

    private String clientIp(HttpServletRequest req) {
        String ip = req.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank())
            ip = req.getRemoteAddr();
        
        ip = ip.replaceAll("[^0-9a-fA-F:\\.]", "");
        
        return ip;
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> body, HttpServletRequest request) {
        // Log inicial de la petición
        log.info("==== [REFRESH TOKEN REQUEST] ====");
        log.info("Endpoint: /auth/refresh");
        log.info("Headers: {}", Collections.list(request.getHeaderNames())
                .stream()
                .collect(Collectors.toMap(h -> h, request::getHeader)));

        // Extraer token del body
        String refreshToken = (body != null) ? body.get("refreshToken") : null;
        log.info("Token recibido en body: {}",
                (refreshToken != null ? "[OK] Length=" + refreshToken.length() : "[NULL]"));

        if (refreshToken == null || refreshToken.isBlank()) {
            log.warn("No se recibió refreshToken en el body.");
            return ResponseEntity.badRequest().body(Map.of("mensaje", "refreshToken requerido"));
        }

        // Validar token con el servicio
        String username = null;
        try {
            username = refreshTokenService.consumeRefreshToken(refreshToken);
            log.info("Usuario extraído del refreshToken: {}", username);
        } catch (Exception ex) {
            log.error("Error al validar el refreshToken: {}", ex.getMessage(), ex);
            return ResponseEntity.status(401).body(Map.of("mensaje", "Error al validar refreshToken"));
        }

        if (username == null) {
            log.warn("Refresh token inválido o expirado.");
            return ResponseEntity.status(401).body(Map.of("mensaje", "refreshToken inválido"));
        }

        EmpleadoDto user;
        try {
            user = empleadoService.loadUserByUsername(username);
            log.info("Usuario encontrado: {}", user != null ? user.getUid() : "NULL");
        } catch (Exception e) {
            log.error("Error al cargar usuario '{}': {}", username, e.getMessage(), e);
            return ResponseEntity.status(400).body(Map.of("mensaje", "Usuario inválido"));
        }

        Jwt newJwt;
        try {
            newJwt = accessTokenFactory.encodeAccessToken(username, user, accessTtlMinutes);
            log.info("Nuevo access token generado correctamente.");
        } catch (Exception ex) {
            log.error("Error al generar nuevo access token: {}", ex.getMessage(), ex);
            return ResponseEntity.status(500).body(Map.of("mensaje", "Error generando access token"));
        }

        String newRefresh;
        try {
            newRefresh = refreshTokenService.createRefreshToken(username);
            log.info("Nuevo refresh token generado correctamente.");
        } catch (Exception ex) {
            log.error("Error al generar nuevo refresh token: {}", ex.getMessage(), ex);
            return ResponseEntity.status(500).body(Map.of("mensaje", "Error generando refresh token"));
        }

        log.info("Usuario: {}", username);
        log.info("AccessToken (inicio): {}...", newJwt.getTokenValue().substring(0, 20));
        log.info("RefreshToken (inicio): {}...", newRefresh.substring(0, 20));
        log.info("Expira en: {} minutos", accessTtlMinutes);

        return ResponseEntity.ok(Map.of(
                "token", newJwt.getTokenValue(),
                "refreshToken", newRefresh,
                "tokenType", "Bearer",
                "expiresIn", String.valueOf(Duration.ofMinutes(accessTtlMinutes).getSeconds())));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {
        String h = request.getHeader("Authorization");
        if (h == null || !h.startsWith("Bearer ")) {
            return ResponseEntity.badRequest().body(Map.of("mensaje", "Authorization Bearer requerido"));
        }
        String token = h.substring("Bearer ".length());
        try {
            Jwt decoded = jwtDecoder.decode(token);

            Instant now = Instant.now();
            long ttlSec = 1L;
            if (decoded.getExpiresAt() != null) {
                ttlSec = Math.max(1, decoded.getExpiresAt().getEpochSecond() - now.getEpochSecond());
            }
            String jti = decoded.getId();
            if (jti != null && ttlSec > 0) {
                refreshTokenService.blacklistJti(jti, ttlSec);
            }
            return ResponseEntity.ok(Map.of("code", "OK", "mensaje", "Sesión cerrada"));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of("mensaje", "Token inválido"));
        }
    }
}