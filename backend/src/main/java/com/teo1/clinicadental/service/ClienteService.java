package com.teo1.clinicadental.service;

import com.teo1.clinicadental.dto.ClienteResponse;
import com.teo1.clinicadental.dto.ClienteUpdateRequest;
import com.teo1.clinicadental.dto.HistorialClinicoRequest;
import com.teo1.clinicadental.dto.HistorialClinicoResponse;
import com.teo1.clinicadental.dto.ListaNegraRequest;
import com.teo1.clinicadental.model.Cliente;
import com.teo1.clinicadental.model.HistorialClinico;
import com.teo1.clinicadental.repository.ClienteRepository;
import com.teo1.clinicadental.repository.HistorialClinicoRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final HistorialClinicoRepository historialClinicoRepository;

    @Transactional(readOnly = true)
    public List<ClienteResponse> listarPacientes() {
        return clienteRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ClienteResponse obtenerPaciente(UUID id) {
        return toResponse(buscarCliente(id));
    }

    @Transactional
    public ClienteResponse actualizarPaciente(UUID id, ClienteUpdateRequest request) {
        Cliente cliente = buscarCliente(id);

        if (request.getTelefono() != null) {
            cliente.getUsuario().setTelefono(request.getTelefono());
        }
        if (request.getDireccion() != null) {
            cliente.setDireccion(request.getDireccion());
        }
        if (request.getFechaNacimiento() != null) {
            cliente.setFechaNacimiento(request.getFechaNacimiento());
        }

        return toResponse(clienteRepository.save(cliente));
    }

    @Transactional
    public ClienteResponse actualizarListaNegra(UUID id, ListaNegraRequest request) {
        Cliente cliente = buscarCliente(id);

        if (Boolean.TRUE.equals(request.getEnListaNegra())
                && (request.getMotivo() == null || request.getMotivo().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El motivo es obligatorio para agregar a la lista negra");
        }

        cliente.setEnListaNegra(Boolean.TRUE.equals(request.getEnListaNegra()));
        cliente.setMotivoListaNegra(cliente.isEnListaNegra() ? request.getMotivo() : null);

        return toResponse(clienteRepository.save(cliente));
    }

    @Transactional(readOnly = true)
    public List<HistorialClinicoResponse> listarHistorial(UUID clienteId) {
        buscarCliente(clienteId);
        return historialClinicoRepository.findByClienteIdOrderByFechaDesc(clienteId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public HistorialClinicoResponse agregarHistorial(UUID clienteId, HistorialClinicoRequest request) {
        Cliente cliente = buscarCliente(clienteId);

        HistorialClinico entrada = HistorialClinico.builder()
                .cliente(cliente)
                .descripcion(request.getDescripcion())
                .build();

        return toResponse(historialClinicoRepository.saveAndFlush(entrada));
    }

    private Cliente buscarCliente(UUID id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Paciente no encontrado"));
    }

    private ClienteResponse toResponse(Cliente cliente) {
        return new ClienteResponse(
                cliente.getId(),
                cliente.getUsuario().getId(),
                cliente.getUsuario().getNombre(),
                cliente.getUsuario().getApellido(),
                cliente.getUsuario().getEmail(),
                cliente.getUsuario().getTelefono(),
                cliente.getDpi(),
                cliente.getDireccion(),
                cliente.getFechaNacimiento(),
                cliente.isEnListaNegra(),
                cliente.getMotivoListaNegra()
        );
    }

    private HistorialClinicoResponse toResponse(HistorialClinico historial) {
        return new HistorialClinicoResponse(
                historial.getId(),
                historial.getCliente().getId(),
                historial.getFecha(),
                historial.getDescripcion()
        );
    }
}
