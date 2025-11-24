package com.bim.seif.models;

import java.time.OffsetDateTime;
import javax.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "empleado_audit", indexes = {
    @Index(name = "ix_empleado_audit_uid", columnList = "uid"),
    @Index(name = "ix_empleado_audit_accion", columnList = "accion"),
    @Index(name = "ix_empleado_audit_created_at", columnList = "created_at")
})
@Getter
@Setter
public class EmpleadoAudit {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(length = 200, nullable = false)
  private String uid;

  @Column(length = 40, nullable = false)
  private String accion;

  @Column(length = 200, nullable = false)
  private String actor;

  @Column(length = 64)
  private String ip;

  @Column(name = "user_agent", length = 400)
  private String userAgent;

  @Lob
  @Column(name = "payload_before", columnDefinition = "NVARCHAR(MAX)")
  private String payloadBefore;

  @Lob
  @Column(name = "payload_after", columnDefinition = "NVARCHAR(MAX)")
  private String payloadAfter;

  @Column(name = "created_at", nullable = false)
  private OffsetDateTime createdAt = OffsetDateTime.now();

  @Column(name = "comentario", length = 1000)
  private String comentario;
}