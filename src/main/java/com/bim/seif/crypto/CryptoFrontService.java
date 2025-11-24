package com.bim.seif.crypto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CryptoFrontService {

    private static final String INFO_STR = "pw-mail";
    private static final int AES_KEY_LEN = 32;     // 256 bits
    private static final int GCM_TAG_BITS = 128;

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Descifra el password en claro usando:
     *  - IKM = SHA-256(password) en HEX → bytes
     *  - salt = nonce (del payload del JWT) en base64url
     *  - info = "pw-mail"
     *  - KDF = HKDF-SHA256 → 32 bytes
     *  - Cifrado = AES/GCM/NoPadding con IV (base64) y ct (base64)
     */
    public Optional<String> decryptPlainFromFront(String hashHex, String jwtToken, String ivB64, String ctB64) {
        try {
            if (isBlank(hashHex) || isBlank(jwtToken) || isBlank(ivB64) || isBlank(ctB64)) {
                return Optional.empty();
            }

            // 1) Extraer nonce del JWT (payload)
            final String[] parts = jwtToken.split("\\.");
            if (parts.length < 2) {
                log.warn("JWT inválido: no tiene payload");
                return Optional.empty();
            }
            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            JsonNode payload = mapper.readTree(payloadBytes);
            String nonceB64u = getText(payload, "nonce");
            if (isBlank(nonceB64u)) {
                log.warn("JWT sin 'nonce'");
                return Optional.empty();
            }
            byte[] salt = Base64.getUrlDecoder().decode(nonceB64u);

            // 2) IKM: hashHex → bytes
            byte[] ikm = hexToBytes(hashHex);

            // 3) HKDF-SHA256: derive key
            byte[] info = INFO_STR.getBytes(StandardCharsets.UTF_8);
            byte[] key = hkdfSha256(ikm, salt, info, AES_KEY_LEN);

            // 4) AES-GCM decrypt
            byte[] iv = Base64.getDecoder().decode(ivB64);
            byte[] ct = Base64.getDecoder().decode(ctB64);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            GCMParameterSpec gcm = new GCMParameterSpec(GCM_TAG_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, gcm);
            byte[] pt = cipher.doFinal(ct);

            String plain = new String(pt, StandardCharsets.UTF_8);
            return Optional.of(plain);
        } catch (Exception e) {
            log.warn("Fallo descifrando plano: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /** HKDF(SHA-256) mínima: RFC 5869 (extract+expand) */
    private static byte[] hkdfSha256(byte[] ikm, byte[] salt, byte[] info, int outLen) throws Exception {
        Objects.requireNonNull(ikm);
        if (salt == null) salt = new byte[0];
        if (info == null) info = new byte[0];

        // Extract
        byte[] prk = hmacSha256(salt, ikm);

        // Expand
        int hashLen = 32;
        int n = (int) Math.ceil((double) outLen / hashLen);
        ByteBuffer okm = ByteBuffer.allocate(n * hashLen);
        byte[] tPrev = new byte[0];

        for (int i = 1; i <= n; i++) {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(prk, "HmacSHA256"));
            mac.update(tPrev);
            mac.update(info);
            mac.update((byte) i);
            tPrev = mac.doFinal();
            okm.put(tPrev);
        }
        byte[] out = new byte[outLen];
        okm.flip();
        okm.get(out, 0, outLen);
        return out;
    }

    private static byte[] hmacSha256(byte[] key, byte[] data) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data);
    }

    private static byte[] hexToBytes(String hex) {
        String s = hex.trim();
        int len = s.length();
        byte[] out = new byte[len / 2];
        for (int i = 0; i < out.length; i++) {
            int hi = Character.digit(s.charAt(i * 2), 16);
            int lo = Character.digit(s.charAt(i * 2 + 1), 16);
            out[i] = (byte) ((hi << 4) + lo);
        }
        return out;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private static String getText(JsonNode node, String field) {
        JsonNode n = node.get(field);
        return (n != null && !n.isNull()) ? n.asText() : null;
    }
}