package com.teo1.clinicadental.service;

import com.teo1.clinicadental.dto.DoctorResponse;
import com.teo1.clinicadental.dto.DoctorUpdateRequest;
import com.teo1.clinicadental.dto.HorarioRequest;
import com.teo1.clinicadental.dto.HorarioResponse;
import com.teo1.clinicadental.model.Doctor;
import com.teo1.clinicadental.model.EstadoUsuario;
import com.teo1.clinicadental.model.Horario;
import com.teo1.clinicadental.repository.DoctorRepository;
import com.teo1.clinicadental.repository.HorarioRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final HorarioRepository horarioRepository;

    @Transactional(readOnly = true)
    public List<DoctorResponse> listarDoctores() {
        return doctorRepository.findByUsuarioEstado(EstadoUsuario.ACTIVO).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DoctorResponse obtenerDoctor(UUID id) {
        return toResponse(buscarDoctor(id));
    }

    @Transactional
    public DoctorResponse actualizarDoctor(UUID id, DoctorUpdateRequest request) {
        Doctor doctor = buscarDoctor(id);

        if (request.getNombre() != null && !request.getNombre().isBlank()) {
            doctor.getUsuario().setNombre(request.getNombre());
        }
        if (request.getApellido() != null && !request.getApellido().isBlank()) {
            doctor.getUsuario().setApellido(request.getApellido());
        }
        if (request.getTelefono() != null) {
            doctor.getUsuario().setTelefono(request.getTelefono());
        }

        doctor.setEspecialidad(request.getEspecialidad());
        doctor.setNumeroColegiado(request.getNumeroColegiado());
        doctor.setNumeroClinica(request.getNumeroClinica());
        return toResponse(doctorRepository.save(doctor));
    }

    @Transactional
    public void desactivarDoctor(UUID id) {
        Doctor doctor = buscarDoctor(id);
        doctor.getUsuario().setEstado(EstadoUsuario.INACTIVO);
        doctorRepository.save(doctor);
    }

    @Transactional(readOnly = true)
    public List<HorarioResponse> listarHorarios(UUID doctorId) {
        buscarDoctor(doctorId);
        return horarioRepository.findByDoctorIdOrderByDiaSemanaAscHoraInicioAsc(doctorId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public HorarioResponse agregarHorario(UUID doctorId, HorarioRequest request, Authentication authentication) {
        Doctor doctor = buscarDoctor(doctorId);
        validarPropietario(doctor, authentication);
        validarHorario(doctorId, request, null);

        Horario horario = Horario.builder()
                .doctor(doctor)
                .diaSemana(request.getDiaSemana())
                .horaInicio(request.getHoraInicio())
                .horaFin(request.getHoraFin())
                .build();

        return toResponse(horarioRepository.save(horario));
    }

    @Transactional
    public HorarioResponse actualizarHorario(UUID doctorId, UUID horarioId, HorarioRequest request, Authentication authentication) {
        Doctor doctor = buscarDoctor(doctorId);
        validarPropietario(doctor, authentication);
        Horario horario = buscarHorario(doctorId, horarioId);
        validarHorario(doctorId, request, horarioId);

        horario.setDiaSemana(request.getDiaSemana());
        horario.setHoraInicio(request.getHoraInicio());
        horario.setHoraFin(request.getHoraFin());

        return toResponse(horarioRepository.save(horario));
    }

    @Transactional
    public void eliminarHorario(UUID doctorId, UUID horarioId, Authentication authentication) {
        Doctor doctor = buscarDoctor(doctorId);
        validarPropietario(doctor, authentication);
        horarioRepository.delete(buscarHorario(doctorId, horarioId));
    }

    private void validarPropietario(Doctor doctor, Authentication authentication) {
        boolean esDoctor = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_DOCTOR".equals(authority.getAuthority()));

        if (esDoctor && !doctor.getUsuario().getId().toString().equals(authentication.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo puedes modificar tus propios horarios");
        }
    }

    private void validarHorario(UUID doctorId, HorarioRequest request, UUID horarioExcluido) {
        if (!request.getHoraFin().isAfter(request.getHoraInicio())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La hora fin debe ser posterior a la hora inicio");
        }

        boolean seTraslapa = horarioRepository.findByDoctorIdAndDiaSemana(doctorId, request.getDiaSemana()).stream()
                .filter(existente -> !existente.getId().equals(horarioExcluido))
                .anyMatch(existente -> request.getHoraInicio().isBefore(existente.getHoraFin())
                        && request.getHoraFin().isAfter(existente.getHoraInicio()));

        if (seTraslapa) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El horario se traslapa con otro horario del mismo dia");
        }
    }

    private Doctor buscarDoctor(UUID id) {
        return doctorRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor no encontrado"));
    }

    private Horario buscarHorario(UUID doctorId, UUID horarioId) {
        return horarioRepository.findById(horarioId)
                .filter(horario -> horario.getDoctor().getId().equals(doctorId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Horario no encontrado"));
    }

    private DoctorResponse toResponse(Doctor doctor) {
        return new DoctorResponse(
                doctor.getId(),
                doctor.getUsuario().getId(),
                doctor.getUsuario().getNombre(),
                doctor.getUsuario().getApellido(),
                doctor.getUsuario().getEmail(),
                doctor.getUsuario().getTelefono(),
                doctor.getEspecialidad(),
                doctor.getNumeroColegiado(),
                doctor.getNumeroClinica(),
                doctor.getCalificacionPromedio(),
                doctor.getUsuario().getEstado().name()
        );
    }

    private HorarioResponse toResponse(Horario horario) {
        return new HorarioResponse(
                horario.getId(),
                horario.getDoctor().getId(),
                horario.getDiaSemana(),
                horario.getHoraInicio(),
                horario.getHoraFin()
        );
    }
}
