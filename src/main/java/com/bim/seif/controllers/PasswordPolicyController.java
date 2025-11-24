package com.bim.seif.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.bim.seif.models.dto.PasswordPolicyDto;
import com.bim.seif.services.PasswordPolicyService;

import java.security.Principal;

@Slf4j
@RestController
@RequestMapping
@RequiredArgsConstructor
public class PasswordPolicyController {

  private final PasswordPolicyService service;

  @GetMapping(value = "/password-policy", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<PasswordPolicyDto> get() {
    log.info("Solicitud para obtener la politica de contrasenas activa.");
    return ResponseEntity.ok(service.getActive());
  }

  @GetMapping(value = "/password-policy/public", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<PasswordPolicyDto> getpublic() {
    log.info("Solicitud publica para obtener la politica de contrasenas activa.");
    return ResponseEntity.ok(service.getActive());
  }

  @PutMapping(value = "/password-policy")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void update(@RequestBody PasswordPolicyDto dto, Principal principal) {
    log.info("Solicitud para actualizar la politica de contrasenas.");
    String who = principal != null ? principal.getName() : "system";
    service.update(dto, who);
  }
}