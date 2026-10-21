package com.teo1.clinicadental.repository;

import com.teo1.clinicadental.model.LevantamientoInasistencia;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LevantamientoInasistenciaRepository extends JpaRepository<LevantamientoInasistencia, UUID> {

    Optional<LevantamientoInasistencia> findFirstByClienteIdOrderByFechaDesc(UUID clienteId);
}
