package com.teo1.clinicadental.service;

import com.teo1.clinicadental.dto.InasistenciasPacienteResponse;
import com.teo1.clinicadental.dto.LevantamientoInasistenciaRequest;
import com.teo1.clinicadental.dto.LevantamientoInasistenciaResponse;
import com.teo1.clinicadental.model.Cliente;
import com.teo1.clinicadental.model.EstadoCita;
import com.teo1.clinicadental.model.LevantamientoInasistencia;
import com.teo1.clinicadental.repository.CitaRepository;
import com.teo1.clinicadental.repository.ClienteRepository;
import com.teo1.clinicadental.repository.LevantamientoInasistenciaRepository;
import com.teo1.clinicadental.repository.UsuarioRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Restriccion por inasistencias (HU-25): con {@code app.citas.max-inasistencias} citas NO_ASISTIO
 * el paciente no puede agendar. Las inasistencias se cuentan desde el ultimo levantamiento, que
 * guarda quien, cuando y por que se levanto.
 */
@Service
public class InasistenciaService {

    private final LevantamientoInasistenciaRepository levantamientoRepository;
    private final CitaRepository citaRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;
    private final Clock clock;
    private final int maxInasistencias;

    public InasistenciaService(
            LevantamientoInasistenciaRepository levantamientoRepository,
            CitaRepository citaRepository,
            ClienteRepository clienteRepository,
            UsuarioRepository usuarioRepository,
            Clock clock,
            @Value("${app.citas.max-inasistencias:3}") int maxInasistencias
    ) {
        this.levantamientoRepository = levantamientoRepository;
        this.citaRepository = citaRepository;
        this.clienteRepository = clienteRepository;
        this.usuarioRepository = usuarioRepository;
        this.clock = clock;
        this.maxInasistencias = maxInasistencias;
    }

    /** Lanza 409 si el paciente llego al limite de inasistencias. Lo usa el agendado de citas. */
    @Transactional(readOnly = true)
    public void validarPuedeAgendar(Cliente cliente) {
        long inasistencias = contarInasistencias(cliente.getId(), ultimoLevantamiento(cliente.getId()));
        if (inasistencias >= maxInasistencias) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El paciente tiene " + inasistencias + " inasistencias y no puede agendar citas "
                            + "hasta que secretaria o administracion le levanten la restriccion");
        }
    }

    @Transactional(readOnly = true)
    public InasistenciasPacienteResponse obtenerEstado(UUID clienteId) {
        Cliente cliente = buscarCliente(clienteId);
        return estado(cliente, ultimoLevantamiento(cliente.getId()));
    }

    @Transactional
    public InasistenciasPacienteResponse levantarRestriccion(
            UUID clienteId, LevantamientoInasistenciaRequest request, Authentication authentication) {
        Cliente cliente = buscarCliente(clienteId);
        long inasistencias = contarInasistencias(cliente.getId(), ultimoLevantamiento(cliente.getId()));
        if (inasistencias < maxInasistencias) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El paciente no tiene la restriccion por inasistencias (" + inasistencias + " de " + maxInasistencias + ")");
        }

        LevantamientoInasistencia levantamiento = levantamientoRepository.save(LevantamientoInasistencia.builder()
                .cliente(cliente)
                .motivo(request.getMotivo().trim())
                .inasistencias((int) inasistencias)
                .levantadoPor(usuarioRepository.getReferenceById(UUID.fromString(authentication.getName())))
                .fecha(LocalDateTime.now(clock))
                .build());

        return estado(cliente, Optional.of(levantamiento));
    }

    private InasistenciasPacienteResponse estado(Cliente cliente, Optional<LevantamientoInasistencia> ultimo) {
        long inasistencias = contarInasistencias(cliente.getId(), ultimo);
        return new InasistenciasPacienteResponse(
                cliente.getId(),
                inasistencias,
                maxInasistencias,
                inasistencias >= maxInasistencias,
                ultimo.map(this::toResponse).orElse(null)
        );
    }

    private long contarInasistencias(UUID clienteId, Optional<LevantamientoInasistencia> ultimo) {
        if (ultimo.isEmpty()) {
            return citaRepository.countByClienteIdAndEstado(clienteId, EstadoCita.NO_ASISTIO);
        }
        LocalDateTime desde = ultimo.get().getFecha();
        return citaRepository.contarPorClienteYEstadoDespuesDe(
                clienteId, EstadoCita.NO_ASISTIO, desde.toLocalDate(), desde.toLocalTime());
    }

    private Optional<LevantamientoInasistencia> ultimoLevantamiento(UUID clienteId) {
        return levantamientoRepository.findFirstByClienteIdOrderByFechaDesc(clienteId);
    }

    private Cliente buscarCliente(UUID clienteId) {
        return clienteRepository.findById(clienteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Paciente no encontrado"));
    }

    private LevantamientoInasistenciaResponse toResponse(LevantamientoInasistencia levantamiento) {
        return new LevantamientoInasistenciaResponse(
                levantamiento.getId(),
                levantamiento.getMotivo(),
                levantamiento.getInasistencias(),
                levantamiento.getLevantadoPor() != null ? levantamiento.getLevantadoPor().getId() : null,
                levantamiento.getFecha()
        );
    }
}
