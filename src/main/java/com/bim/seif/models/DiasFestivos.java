package com.bim.seif.models;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.sql.Timestamp;
import java.time.LocalDateTime;


@Data
@Entity
@Table(name = "dias_festivos")
public class DiasFestivos implements Serializable {

    @Id
    private Timestamp fecha;
    private String motivo;


     // Sobrecarga extra
    public void setFecha(LocalDateTime fecha) {
        this.fecha = (fecha == null) ? null : Timestamp.valueOf(fecha);
    }

    public Timestamp getFecha() {
        return fecha;
    }
}
