package com.teo1.clinicadental.repository;

import com.teo1.clinicadental.model.Doctor;
import com.teo1.clinicadental.model.EstadoUsuario;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DoctorRepository extends JpaRepository<Doctor, UUID> {

    List<Doctor> findByUsuarioEstado(EstadoUsuario estado);

    Optional<Doctor> findByUsuarioId(UUID usuarioId);
}
