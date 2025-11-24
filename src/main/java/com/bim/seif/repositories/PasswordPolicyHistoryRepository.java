package com.bim.seif.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import com.bim.seif.models.PasswordPolicyHistory;

public interface PasswordPolicyHistoryRepository extends JpaRepository<PasswordPolicyHistory, Long> {
    List<PasswordPolicyHistory> findAllByOrderByChangedAtDesc();
    List<PasswordPolicyHistory> findByPolicyIdOrderByChangedAtDesc(Integer policyId);
}
