package com.bim.seif.models;

import lombok.*;
import javax.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "password_reset_token")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** uid del LDAP (correo/usuario) */
    @Column(name = "uid", nullable = false, length = 200)
    private String uid;

    /** token seguro (base64url) */
    @Column(name = "token", nullable = false, length = 300, unique = true)
    private String token;

    /** expiración en UTC (persistida como DATETIMEOFFSET) */
    @Column(name = "expires_at", nullable = false, columnDefinition = "datetimeoffset(3)")
    private Instant expiresAt;

    /** ya usado/invalidado */
    @Column(name = "used", nullable = false)
    private boolean used;

    /** auditoría en UTC (persistida como DATETIMEOFFSET) */
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "datetimeoffset(3)")
    private Instant createdAt;

    @Column(name = "created_by_ip", length = 45)
    private String createdByIp;

    @Column(name = "created_by_ua", length = 255)
    private String createdByUa;

    /** cuándo se consumió (UTC) */
    @Column(name = "used_at", columnDefinition = "datetimeoffset(3)")
    private Instant usedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now(); // UTC real
        }
    }
}