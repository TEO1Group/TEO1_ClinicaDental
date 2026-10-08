package com.teo1.clinicadental.service;

import com.teo1.clinicadental.dto.CitaRequest;
import com.teo1.clinicadental.dto.CitaResponse;
import com.teo1.clinicadental.model.Cita;
import com.teo1.clinicadental.model.Cliente;
import com.teo1.clinicadental.model.DiaSemana;
import com.teo1.clinicadental.model.Doctor;
import com.teo1.clinicadental.model.EstadoCita;
import com.teo1.clinicadental.model.EstadoUsuario;
import com.teo1.clinicadental.model.Rol;
import com.teo1.clinicadental.repository.CitaRepository;
import com.teo1.clinicadental.repository.ClienteRepository;
import com.teo1.clinicadental.repository.DoctorRepository;
import com.teo1.clinicadental.repository.HorarioRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CitaService {

    private static final String MENSAJE_SOLAPE = "El doctor ya tiene una cita agendada en ese horario";
    private static final String INDICE_CITA_DOCTOR_SLOT = "uq_cita_doctor_slot";

    private final CitaRepository citaRepository;
    private final ClienteRepository clienteRepository;
    private final DoctorRepository doctorRepository;
    private final HorarioRepository horarioRepository;
    private final CitaMapper citaMapper;
    private final Clock clock;
    private final long duracionMinutos;

    public CitaService(
            CitaRepository citaRepository,
            ClienteRepository clienteRepository,
            DoctorRepository doctorRepository,
            HorarioRepository horarioRepository,
            CitaMapper citaMapper,
            Clock clock,
            @Value("${app.citas.duracion-minutos:30}") long duracionMinutos
    ) {
        this.citaRepository = citaRepository;
        this.clienteRepository = clienteRepository;
        this.doctorRepository = doctorRepository;
        this.horarioRepository = horarioRepository;
        this.citaMapper = citaMapper;
        this.clock = clock;
        this.duracionMinutos = duracionMinutos;
    }

    @Transactional
    public CitaResponse crearCita(CitaRequest request, Authentication authentication) {
        Cliente cliente = resolverPaciente(request, authentication);
        Doctor doctor = buscarDoctor(request.getIdDoctor());

        if (doctor.getUsuario().getEstado() != EstadoUsuario.ACTIVO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El doctor no esta activo");
        }
        if (cliente.getUsuario().getEstado() != EstadoUsuario.ACTIVO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El paciente no esta activo");
        }
        if (cliente.isEnListaNegra()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El paciente esta en lista negra y no puede agendar citas");
        }

        LocalDate fecha = request.getFecha();
        LocalTime inicio = request.getHora();

        if (!LocalDateTime.of(fecha, inicio).isAfter(LocalDateTime.now(clock))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha y hora de la cita ya pasaron");
        }

        validarDentroDeHorario(doctor.getId(), fecha, inicio);
        validarSinSolape(doctor.getId(), fecha, inicio);

        Cita cita = Cita.builder()
                .cliente(cliente)
                .doctor(doctor)
                .fecha(fecha)
                .hora(inicio)
                .estado(EstadoCita.AGENDADA)
                .notas(request.getNotas() != null && !request.getNotas().isBlank() ? request.getNotas().trim() : null)
                .recordatorioEnviado(false)
                .build();

        try {
            return citaMapper.toResponse(citaRepository.saveAndFlush(cita));
        } catch (DataIntegrityViolationException exception) {
            if (violaIndiceDeSlot(exception)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, MENSAJE_SOLAPE);
            }
            throw exception;
        }
    }

    private Cliente resolverPaciente(CitaRequest request, Authentication authentication) {
        if (tieneRol(authentication, Rol.DOCTOR)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Los doctores no pueden agendar citas");
        }

        if (tieneRol(authentication, Rol.CLIENTE)) {
            return clienteRepository.findByUsuarioId(UUID.fromString(authentication.getName()))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tu usuario no tiene un paciente asociado"));
        }

        if (tieneRol(authentication, Rol.ADMIN) || tieneRol(authentication, Rol.SECRETARIA)) {
            if (request.getIdCliente() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debes indicar el paciente de la cita");
            }
            return buscarCliente(request.getIdCliente());
        }

        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permiso para agendar citas");
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

    private void validarSinSolape(UUID doctorId, LocalDate fecha, LocalTime inicio) {
        LocalTime fin = inicio.plusMinutes(duracionMinutos);

        boolean seSolapa = citaRepository.findByDoctorIdAndFechaAndEstadoNot(doctorId, fecha, EstadoCita.CANCELADA).stream()
                .anyMatch(otra -> inicio.isBefore(otra.getHora().plusMinutes(duracionMinutos))
                        && otra.getHora().isBefore(fin));

        if (seSolapa) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, MENSAJE_SOLAPE);
        }
    }

    private boolean violaIndiceDeSlot(DataIntegrityViolationException exception) {
        String detalle = exception.getMostSpecificCause().getMessage();
        return detalle != null && detalle.contains(INDICE_CITA_DOCTOR_SLOT);
    }

    private boolean tieneRol(Authentication authentication, Rol rol) {
        String rolBuscado = "ROLE_" + rol.name();
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> rolBuscado.equals(authority.getAuthority()));
    }

    private Doctor buscarDoctor(UUID id) {
        return doctorRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor no encontrado"));
    }

    private Cliente buscarCliente(UUID id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Paciente no encontrado"));
    }
}
