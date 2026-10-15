package com.teo1.clinicadental.repository;

import com.teo1.clinicadental.model.BloqueoDoctor;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BloqueoDoctorRepository extends JpaRepository<BloqueoDoctor, UUID> {

    List<BloqueoDoctor> findByDoctorIdOrderByInicioAsc(UUID doctorId);

    List<BloqueoDoctor> findByDoctorIdAndInicioLessThanAndFinGreaterThanOrderByInicioAsc(
            UUID doctorId, LocalDateTime hasta, LocalDateTime desde);

    Optional<BloqueoDoctor> findByIdAndDoctorId(UUID id, UUID doctorId);
}
