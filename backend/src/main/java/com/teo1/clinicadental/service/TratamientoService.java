package com.teo1.clinicadental.service;

import com.teo1.clinicadental.dto.CitaTratamientoRequest;
import com.teo1.clinicadental.dto.CitaTratamientoResponse;
import com.teo1.clinicadental.dto.TratamientoResponse;
import com.teo1.clinicadental.dto.TratamientosPacienteResponse;
import com.teo1.clinicadental.model.Cita;
import com.teo1.clinicadental.model.CitaTratamiento;
import com.teo1.clinicadental.model.EstadoCita;
import com.teo1.clinicadental.model.Rol;
import com.teo1.clinicadental.model.Tratamiento;
import com.teo1.clinicadental.repository.CitaRepository;
import com.teo1.clinicadental.repository.CitaTratamientoRepository;
import com.teo1.clinicadental.repository.ClienteRepository;
import com.teo1.clinicadental.repository.TratamientoRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
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
public class TratamientoService {

    private final TratamientoRepository tratamientoRepository;
    private final CitaTratamientoRepository citaTratamientoRepository;
    private final CitaRepository citaRepository;
    private final ClienteRepository clienteRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<TratamientoResponse> listarCatalogo() {
        return tratamientoRepository.findAllByOrderByNombreAsc().stream()
                .map(tratamiento -> new TratamientoResponse(
                        tratamiento.getId(),
                        tratamiento.getNombre(),
                        tratamiento.getDescripcion(),
                        tratamiento.getDuracionMinutos(),
                        tratamiento.getCosto()))
                .toList();
    }

    @Transactional
    public CitaTratamientoResponse registrar(UUID citaId, CitaTratamientoRequest request, Authentication authentication) {
        Cita cita = buscarCita(citaId);

        if (!esDoctorDeLaCita(cita, authentication)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo el doctor de la cita puede registrar tratamientos");
        }
        if (cita.getEstado() != EstadoCita.ATENDIDA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solo se pueden registrar tratamientos en una cita ATENDIDA y esta cita esta " + cita.getEstado());
        }

        Tratamiento tratamiento = tratamientoRepository.findById(request.getIdTratamiento())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tratamiento no encontrado"));

        BigDecimal costo = request.getCosto() != null ? request.getCosto() : tratamiento.getCosto();
        if (costo.signum() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El costo no puede ser negativo");
        }

        String observaciones = request.getObservaciones() != null && !request.getObservaciones().isBlank()
                ? request.getObservaciones().trim()
                : null;

        CitaTratamiento registro = CitaTratamiento.builder()
                .cita(cita)
                .tratamiento(tratamiento)
                .costo(costo)
                .observaciones(observaciones)
                .fechaRegistro(LocalDateTime.now(clock))
                .build();

        return toResponse(citaTratamientoRepository.save(registro));
    }

    @Transactional(readOnly = true)
    public List<CitaTratamientoResponse> listarPorCita(UUID citaId, Authentication authentication) {
        Cita cita = buscarCita(citaId);

        boolean veTodas = tieneRol(authentication, Rol.ADMIN) || tieneRol(authentication, Rol.SECRETARIA);
        if (!veTodas && !esDoctorDeLaCita(cita, authentication)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo puedes ver los tratamientos de tus citas");
        }

        return citaTratamientoRepository.findByCitaIdOrderByFechaRegistroAsc(citaId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TratamientosPacienteResponse listarPorPaciente(UUID clienteId) {
        if (!clienteRepository.existsById(clienteId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Paciente no encontrado");
        }

        List<CitaTratamientoResponse> tratamientos = citaTratamientoRepository
                .findByCitaClienteIdOrderByCitaFechaDescCitaHoraDescFechaRegistroAsc(clienteId).stream()
                .map(this::toResponse)
                .toList();

        BigDecimal total = tratamientos.stream()
                .map(CitaTratamientoResponse::getCosto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new TratamientosPacienteResponse(total, tratamientos);
    }

    private Cita buscarCita(UUID citaId) {
        return citaRepository.findConRelacionesById(citaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cita no encontrada"));
    }

    private boolean esDoctorDeLaCita(Cita cita, Authentication authentication) {
        return tieneRol(authentication, Rol.DOCTOR)
                && cita.getDoctor().getUsuario().getId().toString().equals(authentication.getName());
    }

    private boolean tieneRol(Authentication authentication, Rol rol) {
        String rolBuscado = "ROLE_" + rol.name();
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> rolBuscado.equals(authority.getAuthority()));
    }

    private CitaTratamientoResponse toResponse(CitaTratamiento registro) {
        Cita cita = registro.getCita();
        return new CitaTratamientoResponse(
                registro.getId(),
                cita.getId(),
                cita.getFecha(),
                cita.getHora(),
                cita.getDoctor().getUsuario().getNombre(),
                cita.getDoctor().getUsuario().getApellido(),
                registro.getTratamiento().getId(),
                registro.getTratamiento().getNombre(),
                registro.getCosto(),
                registro.getObservaciones(),
                registro.getFechaRegistro()
        );
    }
}
