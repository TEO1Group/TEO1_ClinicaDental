package com.teo1.clinicadental.repository;

import com.teo1.clinicadental.model.CitaTratamiento;
import com.teo1.clinicadental.model.EstadoCita;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface ReporteGananciaRepository extends Repository<CitaTratamiento, UUID> {

    @Query("select doctor.id as idDoctor, usuarioDoctor.nombre as nombreDoctor, usuarioDoctor.apellido as apellidoDoctor, "
            + "count(citaTratamiento) as cantidad, sum(citaTratamiento.costo) as total "
            + "from CitaTratamiento citaTratamiento join citaTratamiento.cita cita "
            + "join cita.doctor doctor join doctor.usuario usuarioDoctor "
            + "where cita.estado = :estado and cita.fecha between :desde and :hasta"
            + " and (cita.fecha < :hoy or (cita.fecha = :hoy and cita.hora <= :hora)) "
            + "group by doctor.id, usuarioDoctor.nombre, usuarioDoctor.apellido")
    List<IngresoDoctor> sumarTratamientosPorDoctor(
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta,
            @Param("estado") EstadoCita estado,
            @Param("hoy") LocalDate hoy,
            @Param("hora") LocalTime hora);

    @Query("select sum(abono.monto) from AbonoPlan abono where abono.fecha >= :inicio and abono.fecha < :fin")
    BigDecimal sumarAbonos(@Param("inicio") LocalDateTime inicio, @Param("fin") LocalDateTime fin);

    interface IngresoDoctor {

        UUID getIdDoctor();

        String getNombreDoctor();

        String getApellidoDoctor();

        long getCantidad();

        BigDecimal getTotal();
    }
}
