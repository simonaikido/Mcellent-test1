package com.bim.seif.models;

import lombok.*;
import javax.persistence.*;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "vacaciones_folio_log")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VacacionesFolioLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // FK a vacaciones_asignacion
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asignacion_id", nullable = false)
    private VacacionesAsignacion asignacion;

    @Column(name = "folio", nullable = false, length = 50)
    private String folio;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private TipoInstruccion tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "campo", nullable = false, length = 30)
    private CampoAsignado campo;

    @Column(name = "valor_anterior", nullable = false, length = 255)
    private String valorAnterior;

    @Column(name = "valor_nuevo", nullable = false, length = 255)
    private String valorNuevo;

    @Column(name = "moved_at", nullable = false)
    private OffsetDateTime movedAt;

    @PrePersist
    public void prePersist() {
        if (movedAt == null) movedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }
}