package com.bim.seif.controllers;

import com.bim.seif.crypto.CryptoTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/crypto")
@RequiredArgsConstructor
public class CryptoController {

    private final CryptoTokenService tokenService;

    @GetMapping("/token")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> issueToken() {
        String jwt = tokenService.issueEphemeralToken(900); // 60s de validez
        return ResponseEntity.ok(Map.of("token", jwt));
    }
}