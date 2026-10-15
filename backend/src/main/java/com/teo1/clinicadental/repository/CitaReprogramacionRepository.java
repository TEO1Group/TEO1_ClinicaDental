package com.teo1.clinicadental.repository;

import com.teo1.clinicadental.model.CitaReprogramacion;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CitaReprogramacionRepository extends JpaRepository<CitaReprogramacion, UUID> {

    List<CitaReprogramacion> findByCitaIdOrderByFechaCambioAsc(UUID citaId);
}
