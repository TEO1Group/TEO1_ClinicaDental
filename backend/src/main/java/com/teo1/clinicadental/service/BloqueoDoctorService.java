package com.teo1.clinicadental.service;

import com.teo1.clinicadental.dto.BloqueoDoctorRequest;
import com.teo1.clinicadental.dto.BloqueoDoctorResponse;
import com.teo1.clinicadental.dto.BloqueoVigenteResponse;
import com.teo1.clinicadental.model.BloqueoDoctor;
import com.teo1.clinicadental.model.Doctor;
import com.teo1.clinicadental.model.EstadoCita;
import com.teo1.clinicadental.model.Rol;
import com.teo1.clinicadental.repository.BloqueoDoctorRepository;
import com.teo1.clinicadental.repository.CitaRepository;
import com.teo1.clinicadental.repository.DoctorRepository;
import com.teo1.clinicadental.repository.UsuarioRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BloqueoDoctorService {

    private final BloqueoDoctorRepository bloqueoDoctorRepository;
    private final DoctorRepository doctorRepository;
    private final CitaRepository citaRepository;
    private final UsuarioRepository usuarioRepository;
    private final Clock clock;
    private final long duracionMinutos;

    public BloqueoDoctorService(
            BloqueoDoctorRepository bloqueoDoctorRepository,
            DoctorRepository doctorRepository,
            CitaRepository citaRepository,
            UsuarioRepository usuarioRepository,
            Clock clock,
            @Value("${app.citas.duracion-minutos:30}") long duracionMinutos
    ) {
        this.bloqueoDoctorRepository = bloqueoDoctorRepository;
        this.doctorRepository = doctorRepository;
        this.citaRepository = citaRepository;
        this.usuarioRepository = usuarioRepository;
        this.clock = clock;
        this.duracionMinutos = duracionMinutos;
    }

    @Transactional
    public BloqueoDoctorResponse crearBloqueo(UUID doctorId, BloqueoDoctorRequest request, Authentication authentication) {
        LocalDateTime inicio = request.getInicio();
        LocalDateTime fin = request.getFin();
        if (!inicio.isBefore(fin)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El inicio del bloqueo debe ser anterior al fin");
        }

        Doctor doctor = doctorRepository.findByIdForUpdate(doctorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor no encontrado"));

        long citasEnElPeriodo = citaRepository.findByDoctorIdAndEstadoAndFechaBetweenOrderByFechaAscHoraAsc(
                        doctorId, EstadoCita.AGENDADA, inicio.toLocalDate(), fin.toLocalDate()).stream()
                .filter(cita -> {
                    LocalDateTime inicioCita = LocalDateTime.of(cita.getFecha(), cita.getHora());
                    return inicioCita.isBefore(fin) && inicioCita.plusMinutes(duracionMinutos).isAfter(inicio);
                })
                .count();

        if (citasEnElPeriodo > 0) {
            String citas = citasEnElPeriodo == 1 ? "1 cita agendada" : citasEnElPeriodo + " citas agendadas";
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El doctor tiene " + citas + " en ese periodo; primero deben reprogramarse o cancelarse");
        }

        BloqueoDoctor bloqueo = BloqueoDoctor.builder()
                .doctor(doctor)
                .inicio(inicio)
                .fin(fin)
                .motivo(request.getMotivo().trim())
                .creadoPor(usuarioRepository.getReferenceById(UUID.fromString(authentication.getName())))
                .fechaCreacion(LocalDateTime.now(clock))
                .build();

        return toResponse(bloqueoDoctorRepository.save(bloqueo));
    }

    @Transactional(readOnly = true)
    public List<BloqueoDoctorResponse> listarBloqueos(UUID doctorId, Authentication authentication) {
        boolean veTodos = tieneRol(authentication, Rol.ADMIN) || tieneRol(authentication, Rol.SECRETARIA);
        if (!veTodos && !tieneRol(authentication, Rol.DOCTOR)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permiso para ver los bloqueos");
        }

        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor no encontrado"));

        if (!veTodos && !doctor.getUsuario().getId().toString().equals(authentication.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo puedes ver tus propios bloqueos");
        }

        return bloqueoDoctorRepository.findByDoctorIdOrderByInicioAsc(doctorId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BloqueoVigenteResponse> listarBloqueosVigentes(UUID doctorId) {
        if (!doctorRepository.existsById(doctorId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor no encontrado");
        }

        LocalDateTime ahora = LocalDateTime.now(clock);
        return bloqueoDoctorRepository.findByDoctorIdOrderByInicioAsc(doctorId).stream()
                .filter(bloqueo -> bloqueo.getFin().isAfter(ahora))
                .map(bloqueo -> new BloqueoVigenteResponse(bloqueo.getInicio(), bloqueo.getFin(), bloqueo.getMotivo()))
                .toList();
    }

    @Transactional
    public void eliminarBloqueo(UUID doctorId, UUID bloqueoId) {
        BloqueoDoctor bloqueo = bloqueoDoctorRepository.findByIdAndDoctorId(bloqueoId, doctorId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Bloqueo no encontrado"));
        bloqueoDoctorRepository.delete(bloqueo);
    }

    private boolean tieneRol(Authentication authentication, Rol rol) {
        String rolBuscado = "ROLE_" + rol.name();
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> rolBuscado.equals(authority.getAuthority()));
    }

    private BloqueoDoctorResponse toResponse(BloqueoDoctor bloqueo) {
        return new BloqueoDoctorResponse(
                bloqueo.getId(),
                bloqueo.getDoctor().getId(),
                bloqueo.getInicio(),
                bloqueo.getFin(),
                bloqueo.getMotivo(),
                bloqueo.getCreadoPor() != null ? bloqueo.getCreadoPor().getId() : null,
                bloqueo.getFechaCreacion()
        );
    }
}
