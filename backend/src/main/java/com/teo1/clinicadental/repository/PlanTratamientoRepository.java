package com.teo1.clinicadental.repository;

import com.teo1.clinicadental.model.EstadoCita;
import com.teo1.clinicadental.model.PlanTratamiento;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PlanTratamientoRepository extends JpaRepository<PlanTratamiento, UUID> {

    @EntityGraph(attributePaths = {"cliente", "cliente.usuario", "doctor", "doctor.usuario", "tratamiento"})
    List<PlanTratamiento> findAllByOrderByFechaCreacionDesc();

    @EntityGraph(attributePaths = {"cliente", "cliente.usuario", "doctor", "doctor.usuario", "tratamiento"})
    List<PlanTratamiento> findByDoctorIdOrderByFechaCreacionDesc(UUID doctorId);

    @EntityGraph(attributePaths = {"cliente", "cliente.usuario", "doctor", "doctor.usuario", "tratamiento"})
    Optional<PlanTratamiento> findConRelacionesById(UUID id);

    // Bloquea el plan mientras se agenda una sesion, para que dos pedidos a la vez no superen el total
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select plan from PlanTratamiento plan where plan.id = :id")
    Optional<PlanTratamiento> findByIdForUpdate(@Param("id") UUID id);

    @Query("select cita.plan.id as idPlan, cita.estado as estado, count(cita) as cantidad from Cita cita "
            + "where cita.plan.id in :planes group by cita.plan.id, cita.estado")
    List<ConteoSesiones> contarSesiones(@Param("planes") Collection<UUID> planes);

    interface ConteoSesiones {

        UUID getIdPlan();

        EstadoCita getEstado();

        long getCantidad();
    }
}
