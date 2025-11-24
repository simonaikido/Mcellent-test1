package com.bim.seif.crypto;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class CryptoTokenService {

    private final CryptoHmacKeyProvider keyProvider;
    private static final SecureRandom RNG = new SecureRandom();

    public String issueEphemeralToken(int ttlSeconds) {
        byte[] nonce = new byte[16];
        RNG.nextBytes(nonce);
        String nonceB64u = Base64.getUrlEncoder().withoutPadding().encodeToString(nonce);

        Instant now = Instant.now();
        Instant exp = now.plusSeconds(ttlSeconds);

        return Jwts.builder()
                .claim("nonce", nonceB64u)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .signWith(keyProvider.getSecretKey(), SignatureAlgorithm.HS256)
                .compact();
    }
}
