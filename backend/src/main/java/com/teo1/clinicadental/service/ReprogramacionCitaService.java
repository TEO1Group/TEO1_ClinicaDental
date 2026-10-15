package com.teo1.clinicadental.service;

import com.teo1.clinicadental.dto.CitaResponse;
import com.teo1.clinicadental.dto.ReprogramarCitaRequest;
import com.teo1.clinicadental.model.Cita;
import com.teo1.clinicadental.model.CitaReprogramacion;
import com.teo1.clinicadental.model.Doctor;
import com.teo1.clinicadental.model.EstadoCita;
import com.teo1.clinicadental.model.Rol;
import com.teo1.clinicadental.repository.CitaReprogramacionRepository;
import com.teo1.clinicadental.repository.CitaRepository;
import com.teo1.clinicadental.repository.DoctorRepository;
import com.teo1.clinicadental.repository.UsuarioRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ReprogramacionCitaService {

    private final CitaRepository citaRepository;
    private final CitaReprogramacionRepository citaReprogramacionRepository;
    private final DoctorRepository doctorRepository;
    private final UsuarioRepository usuarioRepository;
    private final DisponibilidadCitaService disponibilidadCitaService;
    private final CitaMapper citaMapper;
    private final Clock clock;

    @Transactional
    public CitaResponse reprogramar(UUID id, ReprogramarCitaRequest request, Authentication authentication) {
        Cita cita = citaRepository.findConRelacionesById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cita no encontrada"));

        UUID usuarioId = UUID.fromString(authentication.getName());
        boolean esCliente = tieneRol(authentication, Rol.CLIENTE);
        validarPermiso(cita, usuarioId, esCliente, authentication);

        if (cita.getEstado() != EstadoCita.AGENDADA) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Solo se puede reprogramar una cita AGENDADA y esta cita esta " + cita.getEstado());
        }

        LocalDate fechaNueva = request.getFecha();
        LocalTime horaNueva = request.getHora();
        if (fechaNueva.equals(cita.getFecha()) && horaNueva.equals(cita.getHora())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La nueva fecha y hora son iguales a las actuales");
        }

        if (esCliente) {
            disponibilidadCitaService.validarAntelacion(cita, "reprogramar");
        }

        Doctor doctor = doctorRepository.findByIdForUpdate(cita.getDoctor().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor no encontrado"));
        disponibilidadCitaService.validar(doctor, fechaNueva, horaNueva, cita.getId());

        LocalDate fechaAnterior = cita.getFecha();
        LocalTime horaAnterior = cita.getHora();
        cita.setFecha(fechaNueva);
        cita.setHora(horaNueva);
        cita.setRecordatorioEnviado(false);

        Cita guardada;
        try {
            guardada = citaRepository.saveAndFlush(cita);
        } catch (DataIntegrityViolationException exception) {
            if (DisponibilidadCitaService.violaIndiceDeSlot(exception)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, DisponibilidadCitaService.MENSAJE_SOLAPE);
            }
            throw exception;
        }

        citaReprogramacionRepository.save(CitaReprogramacion.builder()
                .cita(guardada)
                .fechaAnterior(fechaAnterior)
                .horaAnterior(horaAnterior)
                .fechaNueva(fechaNueva)
                .horaNueva(horaNueva)
                .motivo(request.getMotivo().trim())
                .fechaCambio(LocalDateTime.now(clock))
                .reprogramadoPor(usuarioRepository.getReferenceById(usuarioId))
                .build());

        return citaMapper.toResponse(guardada);
    }

    private void validarPermiso(Cita cita, UUID usuarioId, boolean esCliente, Authentication authentication) {
        if (tieneRol(authentication, Rol.DOCTOR)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Los doctores no pueden reprogramar citas");
        }
        if (esCliente) {
            if (!usuarioId.equals(cita.getCliente().getUsuario().getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permiso para reprogramar esta cita");
            }
            return;
        }
        if (!tieneRol(authentication, Rol.ADMIN) && !tieneRol(authentication, Rol.SECRETARIA)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "No tienes permiso para reprogramar esta cita");
        }
    }

    private boolean tieneRol(Authentication authentication, Rol rol) {
        String rolBuscado = "ROLE_" + rol.name();
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> rolBuscado.equals(authority.getAuthority()));
    }
}
