// PasswordResetTokenRepository.java
package com.bim.seif.repositories;

import com.bim.seif.models.PasswordResetToken;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    // Fuerza CASE-SENSITIVE en SQL Server para no confundir I vs l
    @Query(value = """
        SELECT TOP(1) *
        FROM password_reset_token
        WHERE token = :token COLLATE Latin1_General_100_CS_AS
    """, nativeQuery = true)
    Optional<PasswordResetToken> findByTokenCS(@Param("token") String token);
}