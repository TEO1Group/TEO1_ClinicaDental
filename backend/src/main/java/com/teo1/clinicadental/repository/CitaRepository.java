package com.teo1.clinicadental.repository;

import com.teo1.clinicadental.model.Cita;
import com.teo1.clinicadental.model.EstadoCita;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @EntityGraph(attributePaths = {"cliente", "cliente.usuario", "doctor", "doctor.usuario"})
    List<Cita> findByPlanIdOrderByFechaAscHoraAsc(UUID planId);

    long countByClienteIdAndEstado(UUID clienteId, EstadoCita estado);

    // Citas del paciente en ese estado cuya fecha y hora son posteriores al momento indicado
    @Query("select count(cita) from Cita cita "
            + "where cita.cliente.id = :clienteId and cita.estado = :estado "
            + "and (cita.fecha > :fecha or (cita.fecha = :fecha and cita.hora > :hora))")
    long contarPorClienteYEstadoDespuesDe(
            @Param("clienteId") UUID clienteId,
            @Param("estado") EstadoCita estado,
            @Param("fecha") LocalDate fecha,
            @Param("hora") LocalTime hora);
}
