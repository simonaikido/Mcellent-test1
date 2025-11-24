package com.bim.seif.models.dto;

public record PasswordPolicyDto(
  int minLength,
  int maxLength,
  boolean requireUpper,
  boolean requireNumbers,
  boolean requireSpecial,
  String forbiddenChars,
  Integer stage1Days,
  Integer stage2Days,
  Integer stage3Days
) {}