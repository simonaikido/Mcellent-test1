package com.bim.seif.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bim.seif.models.PasswordPolicy;
import com.bim.seif.models.PasswordPolicyHistory;
import com.bim.seif.models.dto.PasswordPolicyDto;
import com.bim.seif.repositories.PasswordPolicyHistoryRepository;
import com.bim.seif.repositories.PasswordPolicyRepository;

import java.time.OffsetDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordPolicyService {

  private final PasswordPolicyRepository repo;
  private final PasswordPolicyHistoryRepository historyRepo;

  private volatile PasswordPolicy cached;
  private volatile long cachedAtMs;

  public PasswordPolicyDto getActive() {
    PasswordPolicy p = cached;
    if (p == null || (System.currentTimeMillis() - cachedAtMs) > 5 * 60_000) {
      p = repo.findByIsActiveTrue()
          .orElseGet(() -> repo.findTopByOrderByUpdatedAtDesc()
              .orElseGet(this::bootstrapDefault)); // <- nunca null
      cached = p;
      cachedAtMs = System.currentTimeMillis();
    }
    log.info("Politica de contrasena activa obtenida: {}", p);
    return toDto(p);
  }

  /** Crea y persiste una política por defecto marcada como activa. */
  private PasswordPolicy bootstrapDefault() {
    log.warn("No se encontro una politica de contrasena activa. Creando una por defecto.");
    PasswordPolicy def = new PasswordPolicy();
    def.setId(1); // <--- NECESARIO porque no hay @GeneratedValue
    def.setMinLength(8);
    def.setMaxLength(16);
    def.setRequireUpper(true);
    def.setRequireNumbers(true);
    def.setRequireSpecial(false);
    def.setForbiddenChars("', ?, `, &, |");
    def.setStage1Days(30);
    def.setStage2Days(45);
    def.setStage3Days(60);
    def.setActive(true);
    def.setUpdatedBy("system");
    def.setUpdatedAt(OffsetDateTime.now());

    return repo.save(def);
  }

  @Transactional
  public void update(PasswordPolicyDto dto, String updatedBy) {
    PasswordPolicy p = repo.findByIsActiveTrue()
        .orElseGet(this::bootstrapDefault); // << evita orElseThrow()

    // ... (tu mismo código de historial y actualización)
    PasswordPolicyHistory h = new PasswordPolicyHistory();
    h.setPolicyId(p.getId());
    h.setMinLength(p.getMinLength());
    h.setMaxLength(p.getMaxLength());
    h.setRequireUpper(p.isRequireUpper());
    h.setRequireNumbers(p.isRequireNumbers());
    h.setRequireSpecial(p.isRequireSpecial());
    h.setForbiddenChars(p.getForbiddenChars());
    h.setStage1Days(p.getStage1Days());
    h.setStage2Days(p.getStage2Days());
    h.setStage3Days(p.getStage3Days());
    h.setChangedBy(updatedBy);
    historyRepo.save(h);

    p.setMinLength(dto.minLength());
    p.setMaxLength(dto.maxLength());
    p.setRequireUpper(dto.requireUpper());
    p.setRequireNumbers(dto.requireNumbers());
    p.setRequireSpecial(dto.requireSpecial());
    p.setForbiddenChars(dto.forbiddenChars());
    p.setStage1Days(dto.stage1Days());
    p.setStage2Days(dto.stage2Days());
    p.setStage3Days(dto.stage3Days());
    p.setUpdatedBy(updatedBy);
    p.setUpdatedAt(OffsetDateTime.now());

    repo.save(p);
    cached = null;
  }

  private static PasswordPolicyDto toDto(PasswordPolicy p) {
    return new PasswordPolicyDto(
        p.getMinLength(),
        p.getMaxLength(),
        p.isRequireUpper(),
        p.isRequireNumbers(),
        p.isRequireSpecial(),
        p.getForbiddenChars(),
        p.getStage1Days(),
        p.getStage2Days(),
        p.getStage3Days());
  }
}