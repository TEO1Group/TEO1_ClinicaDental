package com.teo1.clinicadental.repository;

import com.teo1.clinicadental.model.Cita;
import com.teo1.clinicadental.model.EstadoCita;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CitaRepository extends JpaRepository<Cita, UUID> {

    List<Cita> findByDoctorIdAndFechaAndEstadoNot(UUID doctorId, LocalDate fecha, EstadoCita estado);

    @EntityGraph(attributePaths = {"cliente", "cliente.usuario", "doctor", "doctor.usuario"})
    Optional<Cita> findConRelacionesById(UUID id);

    @EntityGraph(attributePaths = {"cliente", "cliente.usuario", "doctor", "doctor.usuario"})
    List<Cita> findAllByOrderByFechaAscHoraAsc();

    @EntityGraph(attributePaths = {"cliente", "cliente.usuario", "doctor", "doctor.usuario"})
    List<Cita> findByClienteIdOrderByFechaAscHoraAsc(UUID clienteId);

    @EntityGraph(attributePaths = {"cliente", "cliente.usuario", "doctor", "doctor.usuario"})
    List<Cita> findByDoctorIdOrderByFechaAscHoraAsc(UUID doctorId);

    @EntityGraph(attributePaths = {"cliente", "cliente.usuario", "doctor", "doctor.usuario"})
    List<Cita> findByEstadoAndFechaBetweenOrderByFechaAscHoraAsc(EstadoCita estado, LocalDate desde, LocalDate hasta);

    @EntityGraph(attributePaths = {"cliente", "cliente.usuario", "doctor", "doctor.usuario"})
    List<Cita> findByClienteIdAndEstadoAndFechaBetweenOrderByFechaAscHoraAsc(
            UUID clienteId, EstadoCita estado, LocalDate desde, LocalDate hasta);

    @EntityGraph(attributePaths = {"cliente", "cliente.usuario", "doctor", "doctor.usuario"})
    List<Cita> findByDoctorIdAndEstadoAndFechaBetweenOrderByFechaAscHoraAsc(
            UUID doctorId, EstadoCita estado, LocalDate desde, LocalDate hasta);

    @EntityGraph(attributePaths = {"cliente", "cliente.usuario", "doctor", "doctor.usuario"})
    List<Cita> findByEstadoAndRecordatorioEnviadoFalseAndFechaBetweenOrderByFechaAscHoraAsc(
            EstadoCita estado, LocalDate desde, LocalDate hasta);
}
