package com.bim.seif.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final StringRedisTemplate redis;

    @Value("${security.oauth2.refresh.prefix}")
    private String refreshPrefix;

    @Value("${security.oauth2.blacklist.prefix}")
    private String blacklistPrefix;

    @Value("${security.oauth2.refresh-token.ttl-days}")
    private long refreshTtlDays;

    public String createRefreshToken(String username) {
        String rt = UUID.randomUUID().toString();
        String key = refreshPrefix + rt;
        redis.opsForValue().set(key, username, Duration.ofDays(refreshTtlDays));
        return rt;
    }

    public String consumeRefreshToken(String refreshToken) {
        String key = refreshPrefix + refreshToken;
        String username = redis.opsForValue().get(key);
        if (username != null) {
            // refresh token rotation (invalidar el usado)
            redis.delete(key);
        }
        return username;
    }

    public void blacklistJti(String jti, long secondsToLive) {
        String key = blacklistPrefix + jti;
        redis.opsForValue().set(key, "1", Duration.ofSeconds(secondsToLive));
    }

    public boolean isBlacklisted(String jti) {
        String key = blacklistPrefix + jti;
        return Boolean.TRUE.equals(redis.hasKey(key));
    }
}
