package com.teo1.clinicadental.service;

import com.teo1.clinicadental.dto.DoctorResponse;
import com.teo1.clinicadental.dto.DoctorUpdateRequest;
import com.teo1.clinicadental.dto.HorarioRequest;
import com.teo1.clinicadental.dto.HorarioResponse;
import com.teo1.clinicadental.model.Doctor;
import com.teo1.clinicadental.model.Horario;
import com.teo1.clinicadental.repository.DoctorRepository;
import com.teo1.clinicadental.repository.HorarioRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
        return doctorRepository.findAll().stream()
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
        doctor.setEspecialidad(request.getEspecialidad());
        doctor.setNumeroColegiado(request.getNumeroColegiado());
        doctor.setNumeroClinica(request.getNumeroClinica());
        return toResponse(doctorRepository.save(doctor));
    }

    @Transactional(readOnly = true)
    public List<HorarioResponse> listarHorarios(UUID doctorId) {
        buscarDoctor(doctorId);
        return horarioRepository.findByDoctorIdOrderByDiaSemanaAscHoraInicioAsc(doctorId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public HorarioResponse agregarHorario(UUID doctorId, HorarioRequest request) {
        Doctor doctor = buscarDoctor(doctorId);

        if (!request.getHoraFin().isAfter(request.getHoraInicio())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La hora fin debe ser posterior a la hora inicio");
        }

        Horario horario = Horario.builder()
                .doctor(doctor)
                .diaSemana(request.getDiaSemana())
                .horaInicio(request.getHoraInicio())
                .horaFin(request.getHoraFin())
                .build();

        return toResponse(horarioRepository.save(horario));
    }

    private Doctor buscarDoctor(UUID id) {
        return doctorRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor no encontrado"));
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
                doctor.getCalificacionPromedio()
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
