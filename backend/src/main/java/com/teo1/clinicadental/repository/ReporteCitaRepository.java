package com.teo1.clinicadental.repository;

import com.teo1.clinicadental.model.Cita;
import com.teo1.clinicadental.model.EstadoCita;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface ReporteCitaRepository extends Repository<Cita, UUID> {

    @Query("select cita.doctor.id as idDoctor, cita.estado as estado, count(cita) as cantidad "
            + "from Cita cita "
            + "where cita.fecha between :desde and :hasta "
            + "group by cita.doctor.id, cita.estado")
    List<ConteoCitas> contarPorDoctorYEstado(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    interface ConteoCitas {

        UUID getIdDoctor();

        EstadoCita getEstado();

        long getCantidad();
    }
}
