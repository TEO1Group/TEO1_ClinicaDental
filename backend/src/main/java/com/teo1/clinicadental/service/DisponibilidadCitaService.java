package com.teo1.clinicadental.service;

import com.teo1.clinicadental.model.DiaSemana;
import com.teo1.clinicadental.model.Doctor;
import com.teo1.clinicadental.model.EstadoCita;
import com.teo1.clinicadental.repository.CitaRepository;
import com.teo1.clinicadental.repository.HorarioRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DisponibilidadCitaService {

    static final String MENSAJE_SOLAPE = "El doctor ya tiene una cita agendada en ese horario";

    private final CitaRepository citaRepository;
    private final HorarioRepository horarioRepository;
    private final Clock clock;
    private final long duracionMinutos;

    public DisponibilidadCitaService(
            CitaRepository citaRepository,
            HorarioRepository horarioRepository,
            Clock clock,
            @Value("${app.citas.duracion-minutos:30}") long duracionMinutos
    ) {
        this.citaRepository = citaRepository;
        this.horarioRepository = horarioRepository;
        this.clock = clock;
        this.duracionMinutos = duracionMinutos;
    }

    public void validar(Doctor doctor, LocalDate fecha, LocalTime hora, UUID citaExcluidaId) {
        if (!LocalDateTime.of(fecha, hora).isAfter(LocalDateTime.now(clock))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha y hora de la cita ya pasaron");
        }

        validarDentroDeHorario(doctor.getId(), fecha, hora);
        validarSinSolape(doctor.getId(), fecha, hora, citaExcluidaId);
    }

    private void validarDentroDeHorario(UUID doctorId, LocalDate fecha, LocalTime inicio) {
        LocalTime fin = inicio.plusMinutes(duracionMinutos);
        boolean cruzaMedianoche = !fin.isAfter(inicio);

        boolean dentroDeHorario = !cruzaMedianoche
                && horarioRepository.findByDoctorIdAndDiaSemana(doctorId, DiaSemana.desde(fecha.getDayOfWeek())).stream()
                .anyMatch(horario -> !inicio.isBefore(horario.getHoraInicio())
                        && !fin.isAfter(horario.getHoraFin()));

        if (!dentroDeHorario) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La cita no cabe dentro del horario de atencion del doctor");
        }
    }

    private void validarSinSolape(UUID doctorId, LocalDate fecha, LocalTime inicio, UUID citaExcluidaId) {
        LocalTime fin = inicio.plusMinutes(duracionMinutos);

        boolean seSolapa = citaRepository.findByDoctorIdAndFechaAndEstadoNot(doctorId, fecha, EstadoCita.CANCELADA).stream()
                .filter(otra -> citaExcluidaId == null || !citaExcluidaId.equals(otra.getId()))
                .anyMatch(otra -> inicio.isBefore(otra.getHora().plusMinutes(duracionMinutos))
                        && otra.getHora().isBefore(fin));

        if (seSolapa) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, MENSAJE_SOLAPE);
        }
    }
}
