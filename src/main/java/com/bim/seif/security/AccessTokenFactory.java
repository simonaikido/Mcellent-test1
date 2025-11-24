package com.bim.seif.security;

import com.bim.seif.models.dto.EmpleadoDto;
import lombok.RequiredArgsConstructor;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
@RequiredArgsConstructor
public class AccessTokenFactory {

    private final @Qualifier("hsJwtEncoder") JwtEncoder encoder;

    public Jwt encodeAccessToken(String subject, EmpleadoDto user, int ttlMinutes) {
        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(subject)
                .issuedAt(now)
                .expiresAt(now.plus(ttlMinutes, ChronoUnit.MINUTES))
                .claim("p", user.getRol())
                .claim("r", user.getRegiones())
                .claim("n", user.getNombre())
                .build();

        // Explícito HS256
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256)
                .type("JWT")
                .build();

        return encoder.encode(JwtEncoderParameters.from(header, claims));
    }
}
