package com.bim.seif.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.*;


@Configuration
public class KeyConfig {

  @Value("${JWT_SECRET_BASE64}")
  private String SECRET_KEY;

  private static byte[] decodeKey(String v) {
    v = v.trim();
    if (v.matches("^[0-9A-Fa-f]+$") && v.length() % 2 == 0) { // acepta HEX también
      byte[] out = new byte[v.length() / 2];
      for (int i = 0; i < v.length(); i += 2) out[i/2] = (byte) Integer.parseInt(v.substring(i, i+2), 16);
      return out;
    }
    return java.util.Base64.getDecoder().decode(v);
  }

  @Bean(name = "hsJwtEncoder")
  @Primary
  public JwtEncoder hsJwtEncoder() {
    byte[] keyBytes = decodeKey(SECRET_KEY);
    var key = new javax.crypto.spec.SecretKeySpec(keyBytes, "HmacSHA256");
    return new NimbusJwtEncoder(new com.nimbusds.jose.jwk.source.ImmutableSecret<>(key));
  }

  @Bean(name = "hsJwtDecoder")
  @Primary
  public JwtDecoder hsJwtDecoder() {
    byte[] keyBytes = decodeKey(SECRET_KEY);
    var key = new javax.crypto.spec.SecretKeySpec(keyBytes, "HmacSHA256");
    return NimbusJwtDecoder.withSecretKey(key).build();
  }
}