package com.teo1.clinicadental.service;

import com.teo1.clinicadental.dto.CitaRequest;
import com.teo1.clinicadental.dto.CitaResponse;
import com.teo1.clinicadental.dto.EstadoCitaRequest;
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
import java.util.List;
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
    private static final int MAXIMO_HORAS_PROXIMAS = 72;

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

    @Transactional(readOnly = true)
    public List<CitaResponse> listarCitas(
            LocalDate fecha,
            UUID idDoctor,
            UUID idCliente,
            EstadoCita estado,
            Authentication authentication
    ) {
        return citasVisibles(authentication).stream()
                .filter(cita -> fecha == null || fecha.equals(cita.getFecha()))
                .filter(cita -> idDoctor == null || idDoctor.equals(cita.getDoctor().getId()))
                .filter(cita -> idCliente == null || idCliente.equals(cita.getCliente().getId()))
                .filter(cita -> estado == null || estado == cita.getEstado())
                .map(citaMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CitaResponse> listarProximas(int horas, Authentication authentication) {
        if (horas < 1 || horas > MAXIMO_HORAS_PROXIMAS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Las horas deben estar entre 1 y " + MAXIMO_HORAS_PROXIMAS);
        }

        LocalDateTime ahora = LocalDateTime.now(clock);
        LocalDateTime limite = ahora.plusHours(horas);

        // fecha y hora estan en columnas separadas: se trae por dias y se filtra la ventana exacta aqui
        return proximasVisibles(ahora.toLocalDate(), limite.toLocalDate(), authentication).stream()
                .filter(cita -> {
                    LocalDateTime inicio = LocalDateTime.of(cita.getFecha(), cita.getHora());
                    return !inicio.isBefore(ahora) && !inicio.isAfter(limite);
                })
                .map(citaMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CitaResponse obtenerCita(UUID id, Authentication authentication) {
        Cita cita = citaRepository.findConRelacionesById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cita no encontrada"));

        if (!puedeVerCita(cita, authentication)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permiso para ver esta cita");
        }

        return citaMapper.toResponse(cita);
    }

    @Transactional
    public CitaResponse cambiarEstado(UUID id, EstadoCitaRequest request, Authentication authentication) {
        EstadoCita nuevoEstado = request.getEstado();
        if (nuevoEstado == EstadoCita.AGENDADA) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Una cita no puede volver a AGENDADA");
        }

        Cita cita = citaRepository.findConRelacionesById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cita no encontrada"));

        validarPermisoCambioEstado(cita, nuevoEstado, authentication);

        if (cita.getEstado() != EstadoCita.AGENDADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La cita ya esta " + cita.getEstado() + " y no puede cambiar de estado");
        }

        cita.setEstado(nuevoEstado);
        return citaMapper.toResponse(citaRepository.save(cita));
    }

    // CANCELADA: CLIENTE (su cita), SECRETARIA y ADMIN. ATENDIDA y NO_ASISTIO: DOCTOR (su cita) y ADMIN
    private void validarPermisoCambioEstado(Cita cita, EstadoCita nuevoEstado, Authentication authentication) {
        if (tieneRol(authentication, Rol.ADMIN)) {
            return;
        }

        UUID usuarioId = UUID.fromString(authentication.getName());
        boolean permitido = nuevoEstado == EstadoCita.CANCELADA
                ? tieneRol(authentication, Rol.SECRETARIA)
                        || (tieneRol(authentication, Rol.CLIENTE) && usuarioId.equals(cita.getCliente().getUsuario().getId()))
                : tieneRol(authentication, Rol.DOCTOR) && usuarioId.equals(cita.getDoctor().getUsuario().getId());

        if (!permitido) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permiso para marcar esta cita como " + nuevoEstado);
        }
    }

    // CLIENTE y DOCTOR ven solo sus citas; SECRETARIA y ADMIN ven todas
    private List<Cita> citasVisibles(Authentication authentication) {
        if (veTodasLasCitas(authentication)) {
            return citaRepository.findAllByOrderByFechaAscHoraAsc();
        }

        UUID usuarioId = UUID.fromString(authentication.getName());
        if (tieneRol(authentication, Rol.CLIENTE)) {
            return clienteRepository.findByUsuarioId(usuarioId)
                    .map(paciente -> citaRepository.findByClienteIdOrderByFechaAscHoraAsc(paciente.getId()))
                    .orElse(List.of());
        }
        if (tieneRol(authentication, Rol.DOCTOR)) {
            return doctorRepository.findByUsuarioId(usuarioId)
                    .map(medico -> citaRepository.findByDoctorIdOrderByFechaAscHoraAsc(medico.getId()))
                    .orElse(List.of());
        }
        return List.of();
    }

    private List<Cita> proximasVisibles(LocalDate desde, LocalDate hasta, Authentication authentication) {
        if (veTodasLasCitas(authentication)) {
            return citaRepository.findByEstadoAndFechaBetweenOrderByFechaAscHoraAsc(EstadoCita.AGENDADA, desde, hasta);
        }

        UUID usuarioId = UUID.fromString(authentication.getName());
        if (tieneRol(authentication, Rol.CLIENTE)) {
            return clienteRepository.findByUsuarioId(usuarioId)
                    .map(paciente -> citaRepository.findByClienteIdAndEstadoAndFechaBetweenOrderByFechaAscHoraAsc(
                            paciente.getId(), EstadoCita.AGENDADA, desde, hasta))
                    .orElse(List.of());
        }
        if (tieneRol(authentication, Rol.DOCTOR)) {
            return doctorRepository.findByUsuarioId(usuarioId)
                    .map(medico -> citaRepository.findByDoctorIdAndEstadoAndFechaBetweenOrderByFechaAscHoraAsc(
                            medico.getId(), EstadoCita.AGENDADA, desde, hasta))
                    .orElse(List.of());
        }
        return List.of();
    }

    private boolean puedeVerCita(Cita cita, Authentication authentication) {
        if (veTodasLasCitas(authentication)) {
            return true;
        }

        UUID usuarioId = UUID.fromString(authentication.getName());
        if (tieneRol(authentication, Rol.CLIENTE)) {
            return usuarioId.equals(cita.getCliente().getUsuario().getId());
        }
        if (tieneRol(authentication, Rol.DOCTOR)) {
            return usuarioId.equals(cita.getDoctor().getUsuario().getId());
        }
        return false;
    }

    private boolean veTodasLasCitas(Authentication authentication) {
        return tieneRol(authentication, Rol.ADMIN) || tieneRol(authentication, Rol.SECRETARIA);
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
