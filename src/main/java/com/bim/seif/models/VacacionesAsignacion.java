package com.bim.seif.models;

import lombok.*;
import javax.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "vacaciones_asignacion")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VacacionesAsignacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "titular_email", nullable = false, length = 255)
    private String titularEmail;

    @Column(name = "sustituto_email", nullable = false, length = 255)
    private String sustitutoEmail;

    @Column(name = "fecha_inicio", nullable = false)
    private OffsetDateTime fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private OffsetDateTime fechaFin;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 20)
    private EstadoAsignacion estado;

    @Column(name = "auto_revert", nullable = false)
    private boolean autoRevert;

    @Column(name = "desactivado_titular", nullable = false)
    private boolean desactivadoTitular;

    @Column(name = "creado_por", nullable = false, length = 255)
    private String creadoPor;

    @Column(name = "motivo", length = 500)
    private String motivo;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (estado == null) estado = EstadoAsignacion.ACTIVA;
        if (createdAt == null) createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        // normalizamos a límites de día si te acomoda (opcional)
        if (fechaInicio != null) fechaInicio = fechaInicio.withHour(0).withMinute(0).withSecond(0).withNano(0);
        if (fechaFin != null) fechaFin = fechaFin.withHour(23).withMinute(59).withSecond(59).withNano(0);
    }

    /** helpers opcionales por si construyes con LocalDate en el servicio */
    public static OffsetDateTime startOfDayUTC(LocalDate d) {
        return d == null ? null : d.atStartOfDay().atOffset(ZoneOffset.UTC);
    }

    public static OffsetDateTime endOfDayUTC(LocalDate d) {
        return d == null ? null : d.atTime(23,59,59).atOffset(ZoneOffset.UTC);
    }
}