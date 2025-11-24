package com.bim.seif.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bim.seif.models.PasswordPolicyHistory;
import com.bim.seif.repositories.PasswordPolicyHistoryRepository;

import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordPolicyHistoryService {

  private final PasswordPolicyHistoryRepository historyRepo;

  /**
   * Guarda una nueva entrada en el historial de políticas de contraseña.
   */
  @Transactional
  public void registrarCambio(PasswordPolicyHistory history) {

    log.info("Registrando cambio en la politica de contrasena: {}", history);
    if (history.getChangedAt() == null) {
      history.setChangedAt(OffsetDateTime.now());
    }
    historyRepo.save(history);
  }

  /**
   * Devuelve el historial completo de cambios, ordenado del más reciente al más antiguo.
   */
  @Transactional(readOnly = true)
  public List<PasswordPolicyHistory> obtenerHistorial() {
    log.info("Obteniendo historial de cambios en políticas de contrasena");
    return historyRepo.findAllByOrderByChangedAtDesc();
  }

  /**
   * Devuelve el historial para una política específica.
   */
  @Transactional(readOnly = true)
  public List<PasswordPolicyHistory> obtenerHistorialPorPolicyId(Integer policyId) {
    log.info("Obteniendo historial de cambios en politicas de contrasena para policyId: {}", policyId);
    return historyRepo.findByPolicyIdOrderByChangedAtDesc(policyId);
  }
}