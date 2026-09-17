package com.teo1.clinicadental.repository;

import com.teo1.clinicadental.model.Horario;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HorarioRepository extends JpaRepository<Horario, UUID> {

    List<Horario> findByDoctorIdOrderByDiaSemanaAscHoraInicioAsc(UUID doctorId);
}
