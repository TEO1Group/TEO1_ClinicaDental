package com.teo1.clinicadental.repository;

import com.teo1.clinicadental.model.HistorialClinico;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistorialClinicoRepository extends JpaRepository<HistorialClinico, UUID> {

    List<HistorialClinico> findByClienteIdOrderByFechaDesc(UUID clienteId);
}
