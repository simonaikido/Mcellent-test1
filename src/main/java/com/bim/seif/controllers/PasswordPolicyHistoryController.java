package com.bim.seif.controllers;

import com.bim.seif.models.PasswordPolicyHistory;
import com.bim.seif.services.PasswordPolicyHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/password-policy/history")
@RequiredArgsConstructor
public class PasswordPolicyHistoryController {

  private final PasswordPolicyHistoryService historyService;

  /**
   * GET /password-policy/history
   * Retorna todo el historial de cambios ordenado por fecha descendente.
   */
  @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<List<PasswordPolicyHistory>> getAll() {
    List<PasswordPolicyHistory> history = historyService.obtenerHistorial();
    log.info("Se recuperaron {} registros de historial de cambios.", history.size());
    return history.isEmpty()
        ? ResponseEntity.noContent().build()
        : ResponseEntity.ok(history);
  }

  /**
   * GET /password-policy/history/{policyId}
   * Retorna el historial de cambios para una política específica.
   */
  @GetMapping(value = "/{policyId}", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<List<PasswordPolicyHistory>> getByPolicyId(@PathVariable Integer policyId) {
    List<PasswordPolicyHistory> history = historyService.obtenerHistorialPorPolicyId(policyId);
    log.info("Se recuperaron {} registros de historial de cambios para la politica con ID: {}.", history.size(), policyId);
    return history.isEmpty()
        ? ResponseEntity.noContent().build()
        : ResponseEntity.ok(history);
  }
}