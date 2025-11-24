package com.bim.seif.repositories;

import com.bim.seif.models.Notificacion;
import com.bim.seif.models.TipoNotificacion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificacionRepository extends JpaRepository<Notificacion, TipoNotificacion> {
}
