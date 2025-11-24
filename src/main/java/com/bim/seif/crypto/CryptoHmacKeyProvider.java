package com.bim.seif.crypto;

import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.crypto.SecretKey;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class CryptoHmacKeyProvider {

    @Value("${crypto.token.hmacSecret}")
    private String hmacSecretFromCfg;

    @Getter
    private SecretKey secretKey; // HS256 key

    @PostConstruct
    public void init() {
        if (hmacSecretFromCfg != null && !hmacSecretFromCfg.trim().isEmpty()) {
            // Si lo cargas en Base64 (recomendado):
            byte[] bytes = decodeMaybeBase64(hmacSecretFromCfg.trim());
            this.secretKey = Keys.hmacShaKeyFor(bytes);
        } else {
            // Genera uno aleatorio al arrancar (se invalida al reiniciar)
            byte[] bytes = new byte[64]; // 512 bits
            new SecureRandom().nextBytes(bytes);
            this.secretKey = Keys.hmacShaKeyFor(bytes);
        }
    }

    private byte[] decodeMaybeBase64(String s) {
        try {
            return Base64.getDecoder().decode(s);
        } catch (IllegalArgumentException ex) {
            // No era base64; usa bytes directos del string
            return s.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        }
    }
}