package com.teo1.clinicadental.controller;

import com.teo1.clinicadental.dto.ClienteResponse;
import com.teo1.clinicadental.dto.ClienteUpdateRequest;
import com.teo1.clinicadental.dto.HistorialClinicoRequest;
import com.teo1.clinicadental.dto.HistorialClinicoResponse;
import com.teo1.clinicadental.dto.ListaNegraRequest;
import com.teo1.clinicadental.service.ClienteService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pacientes")
@RequiredArgsConstructor
public class ClienteController {

    private final ClienteService clienteService;

    @GetMapping
    public ResponseEntity<List<ClienteResponse>> listarPacientes() {
        return ResponseEntity.ok(clienteService.listarPacientes());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClienteResponse> obtenerPaciente(@PathVariable UUID id) {
        return ResponseEntity.ok(clienteService.obtenerPaciente(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClienteResponse> actualizarPaciente(
            @PathVariable UUID id,
            @Valid @RequestBody ClienteUpdateRequest request
    ) {
        return ResponseEntity.ok(clienteService.actualizarPaciente(id, request));
    }

    @PatchMapping("/{id}/lista-negra")
    public ResponseEntity<ClienteResponse> actualizarListaNegra(
            @PathVariable UUID id,
            @Valid @RequestBody ListaNegraRequest request
    ) {
        return ResponseEntity.ok(clienteService.actualizarListaNegra(id, request));
    }

    @GetMapping("/{id}/historial")
    public ResponseEntity<List<HistorialClinicoResponse>> listarHistorial(@PathVariable UUID id) {
        return ResponseEntity.ok(clienteService.listarHistorial(id));
    }

    @PostMapping("/{id}/historial")
    public ResponseEntity<HistorialClinicoResponse> agregarHistorial(
            @PathVariable UUID id,
            @Valid @RequestBody HistorialClinicoRequest request
    ) {
        HistorialClinicoResponse response = clienteService.agregarHistorial(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
