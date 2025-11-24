package com.bim.seif.utils;

import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LogsUtils {

    private final JwtDecoder jwtDecoder;

    public LogsUtils(@Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:}") String issuerUri,
                     @Value("${JWT_SECRET_BASE64:}") String secret) {
        // Si tienes issuer-uri, úsalo. Si no, usa el secret HMAC como fallback
        if (issuerUri != null && !issuerUri.isEmpty()) {
            this.jwtDecoder = JwtDecoders.fromIssuerLocation(issuerUri);
        } else {
            byte[] keyBytes = java.util.Base64.getDecoder().decode(secret);
            javax.crypto.SecretKey key = new javax.crypto.spec.SecretKeySpec(keyBytes, "HmacSHA256");
            this.jwtDecoder = org.springframework.security.oauth2.jwt.NimbusJwtDecoder.withSecretKey(key).build();
        }
    }

    public String logUserAndIpFromHead(HttpServletRequest request) {
        String ip = extractIp(request);
        String jwtToken = extractToken(request.getHeader("Authorization"));

        String user = decodeUsername(jwtToken);
        return String.format("User: %s, IP: %s", user, ip);
    }

    public String logUserAndIpFromHead(HttpHeaders headers) {
        String jwtToken = extractToken(headers.getFirst(HttpHeaders.AUTHORIZATION));
        String user = decodeUsername(jwtToken);
        return String.format("User: %s, IP: desconocida", user);
    }

    // -------------------- Métodos auxiliares --------------------

    private String extractIp(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        return (xfHeader == null || xfHeader.isEmpty()) ? request.getRemoteAddr() : xfHeader.split(",")[0];
    }

    private String extractToken(String header) {
        if (header == null || !header.startsWith("Bearer ")) return "";
        return header.substring(7);
    }

    private String decodeUsername(String token) {
        if (token == null || token.isEmpty()) return "anónimo";
        try {
            Jwt jwt = jwtDecoder.decode(token);
            return jwt.getSubject(); // "sub" el email o username del usuario
        } catch (Exception e) {
            return "token inválido";
        }
    }
}
