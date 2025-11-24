package com.bim.seif.models;

import java.time.OffsetDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.persistence.Version;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

@Entity
@Table(
  name = "PasswordPolicy",
  uniqueConstraints = {
    // Garantiza que solo exista una fila activa
    @UniqueConstraint(name = "UX_PasswordPolicy_Active", columnNames = {"is_active"})
  }
)
public class PasswordPolicy {

  @Id
  private Integer id;                 // Fijamos id=1 (single row)

  @Column(name = "min_length", nullable = false)
  @Min(4) @Max(128)
  private int minLength;

  @Column(name = "max_length", nullable = false)
  @Min(4) @Max(256)
  private int maxLength;

  @Column(name = "require_upper", nullable = false)
  private boolean requireUpper = true;

  @Column(name = "require_numbers", nullable = false)
  private boolean requireNumbers = true;

  @Column(name = "require_special", nullable = false)
  private boolean requireSpecial = false;

  @Column(name = "forbidden_chars", length = 200)
  private String forbiddenChars;

  @Column(name = "stage1_days")
  private Integer stage1Days;

  @Column(name = "stage2_days")
  private Integer stage2Days;

  @Column(name = "stage3_days")
  private Integer stage3Days;

  @Column(name = "updated_by", length = 100)
  private String updatedBy;

  @Column(name = "updated_at", nullable = false)
  private OffsetDateTime updatedAt = OffsetDateTime.now();

  @Column(name = "is_active", nullable = false)
  private boolean isActive = true;

  @Version
  private Long version;               // Optimistic locking

  public Integer getId() {
    return id;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public int getMinLength() {
    return minLength;
  }

  public void setMinLength(int minLength) {
    this.minLength = minLength;
  }

  public int getMaxLength() {
    return maxLength;
  }

  public void setMaxLength(int maxLength) {
    this.maxLength = maxLength;
  }

  public boolean isRequireUpper() {
    return requireUpper;
  }

  public void setRequireUpper(boolean requireUpper) {
    this.requireUpper = requireUpper;
  }

  public boolean isRequireNumbers() {
    return requireNumbers;
  }

  public void setRequireNumbers(boolean requireNumbers) {
    this.requireNumbers = requireNumbers;
  }

  public boolean isRequireSpecial() {
    return requireSpecial;
  }

  public void setRequireSpecial(boolean requireSpecial) {
    this.requireSpecial = requireSpecial;
  }

  public String getForbiddenChars() {
    return forbiddenChars;
  }

  public void setForbiddenChars(String forbiddenChars) {
    this.forbiddenChars = forbiddenChars;
  }

  public Integer getStage1Days() {
    return stage1Days;
  }

  public void setStage1Days(Integer stage1Days) {
    this.stage1Days = stage1Days;
  }

  public Integer getStage2Days() {
    return stage2Days;
  }

  public void setStage2Days(Integer stage2Days) {
    this.stage2Days = stage2Days;
  }

  public Integer getStage3Days() {
    return stage3Days;
  }

  public void setStage3Days(Integer stage3Days) {
    this.stage3Days = stage3Days;
  }

  public String getUpdatedBy() {
    return updatedBy;
  }

  public void setUpdatedBy(String updatedBy) {
    this.updatedBy = updatedBy;
  }

  public OffsetDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(OffsetDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  public boolean isActive() {
    return isActive;
  }

  public void setActive(boolean isActive) {
    this.isActive = isActive;
  }

  public Long getVersion() {
    return version;
  }

  public void setVersion(Long version) {
    this.version = version;
  }

  // getters & setters
  
}