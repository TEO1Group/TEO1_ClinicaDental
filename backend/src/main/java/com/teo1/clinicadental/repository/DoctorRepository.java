package com.teo1.clinicadental.repository;

import com.teo1.clinicadental.model.Doctor;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DoctorRepository extends JpaRepository<Doctor, UUID> {
}
