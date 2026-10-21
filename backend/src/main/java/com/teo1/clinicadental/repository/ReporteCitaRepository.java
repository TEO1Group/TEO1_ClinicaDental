package com.teo1.clinicadental.repository;

import com.teo1.clinicadental.model.Cita;
import com.teo1.clinicadental.model.EstadoCita;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
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

    @Query("select hour(cita.hora) as hora, count(cita) as cantidad from Cita cita "
            + "where cita.fecha between :desde and :hasta group by hour(cita.hora) order by hour(cita.hora)")
    List<ConteoHora> contarPorHora(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    // Citas de los estados indicados agrupadas por doctor, paciente y estado, con los nombres de ambos
    @Query("select doctor.id as idDoctor, usuarioDoctor.nombre as nombreDoctor, usuarioDoctor.apellido as apellidoDoctor, "
            + "cliente.id as idCliente, usuarioCliente.nombre as nombreCliente, usuarioCliente.apellido as apellidoCliente, "
            + "cita.estado as estado, count(cita) as cantidad "
            + "from Cita cita join cita.doctor doctor join doctor.usuario usuarioDoctor "
            + "join cita.cliente cliente join cliente.usuario usuarioCliente "
            + "where cita.fecha between :desde and :hasta and cita.estado in :estados "
            + "group by doctor.id, usuarioDoctor.nombre, usuarioDoctor.apellido, "
            + "cliente.id, usuarioCliente.nombre, usuarioCliente.apellido, cita.estado")
    List<ConteoPorDoctorYPaciente> contarPorDoctorYPaciente(
            @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta,
            @Param("estados") Collection<EstadoCita> estados);

    // Tratamientos registrados en citas del periodo: cantidad y suma del costo cobrado
    @Query("select tratamiento.id as idTratamiento, tratamiento.nombre as nombre, "
            + "count(citaTratamiento) as cantidad, sum(citaTratamiento.costo) as total "
            + "from CitaTratamiento citaTratamiento join citaTratamiento.tratamiento tratamiento "
            + "join citaTratamiento.cita cita "
            + "where cita.fecha between :desde and :hasta "
            + "group by tratamiento.id, tratamiento.nombre "
            + "order by sum(citaTratamiento.costo) desc, tratamiento.nombre")
    List<IngresoTratamiento> sumarIngresosPorTratamiento(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    interface ConteoPorDoctorYPaciente {

        UUID getIdDoctor();

        String getNombreDoctor();

        String getApellidoDoctor();

        UUID getIdCliente();

        String getNombreCliente();

        String getApellidoCliente();

        EstadoCita getEstado();

        long getCantidad();
    }

    interface IngresoTratamiento {

        UUID getIdTratamiento();

        String getNombre();

        long getCantidad();

        BigDecimal getTotal();
    }

    interface ConteoHora {
        int getHora();
        long getCantidad();
    }

    interface ConteoCitas {

        UUID getIdDoctor();

        EstadoCita getEstado();

        long getCantidad();
    }
}
