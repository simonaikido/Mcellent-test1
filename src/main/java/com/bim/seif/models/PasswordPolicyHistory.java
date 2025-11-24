package com.bim.seif.models;

import java.time.OffsetDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "PasswordPolicyHistory")
public class PasswordPolicyHistory {

  @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long historyId;

  @Column(nullable = false)
  private Integer policyId;

  private int minLength;
  private int maxLength;
  private boolean requireUpper;
  private boolean requireNumbers;
  private boolean requireSpecial;
  private String forbiddenChars;
  private Integer stage1Days;
  private Integer stage2Days;
  private Integer stage3Days;

  private String changedBy;

  @Column(nullable = false)
  private OffsetDateTime changedAt = OffsetDateTime.now();

  // getters & setters
  public Long getHistoryId() {
    return historyId;
  }

  public void setHistoryId(Long historyId) {
    this.historyId = historyId;
  }

  public Integer getPolicyId() {
    return policyId;
  }

  public void setPolicyId(Integer policyId) {
    this.policyId = policyId;
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

  public String getChangedBy() {
    return changedBy;
  }

  public void setChangedBy(String changedBy) {
    this.changedBy = changedBy;
  }

  public OffsetDateTime getChangedAt() {
    return changedAt;
  }

  public void setChangedAt(OffsetDateTime changedAt) {
    this.changedAt = changedAt;
  }
}