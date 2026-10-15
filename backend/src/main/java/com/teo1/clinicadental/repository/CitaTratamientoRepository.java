package com.teo1.clinicadental.repository;

import com.teo1.clinicadental.model.CitaTratamiento;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CitaTratamientoRepository extends JpaRepository<CitaTratamiento, UUID> {

    @EntityGraph(attributePaths = {"tratamiento", "cita", "cita.doctor", "cita.doctor.usuario"})
    List<CitaTratamiento> findByCitaIdOrderByFechaRegistroAsc(UUID citaId);

    @EntityGraph(attributePaths = {"tratamiento", "cita", "cita.doctor", "cita.doctor.usuario"})
    List<CitaTratamiento> findByCitaClienteIdOrderByCitaFechaDescCitaHoraDescFechaRegistroAsc(UUID clienteId);
}
