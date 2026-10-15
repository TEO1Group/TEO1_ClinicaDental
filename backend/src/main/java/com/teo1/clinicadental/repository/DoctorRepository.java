package com.teo1.clinicadental.repository;

import com.teo1.clinicadental.model.Doctor;
import com.teo1.clinicadental.model.EstadoUsuario;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DoctorRepository extends JpaRepository<Doctor, UUID> {

    @EntityGraph(attributePaths = {"usuario", "usuario.rol"})
    List<Doctor> findByUsuarioEstado(EstadoUsuario estado);

    @EntityGraph(attributePaths = {"usuario", "usuario.rol"})
    Optional<Doctor> findByUsuarioId(UUID usuarioId);

    @EntityGraph(attributePaths = {"usuario", "usuario.rol"})
    @Override
    Optional<Doctor> findById(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select doctor from Doctor doctor where doctor.id = :id")
    Optional<Doctor> findByIdForUpdate(@Param("id") UUID id);
}
