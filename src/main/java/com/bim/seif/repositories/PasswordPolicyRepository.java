package com.bim.seif.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import com.bim.seif.models.PasswordPolicy;

import java.util.Optional;

public interface PasswordPolicyRepository extends JpaRepository<PasswordPolicy, Integer> {
  Optional<PasswordPolicy> findByIsActiveTrue();
  Optional<PasswordPolicy> findTopByOrderByUpdatedAtDesc();
}