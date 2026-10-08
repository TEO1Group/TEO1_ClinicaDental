package com.teo1.clinicadental.controller;

import com.teo1.clinicadental.dto.ClienteResponse;
import com.teo1.clinicadental.dto.ClienteUpdateRequest;
import com.teo1.clinicadental.dto.HistorialClinicoRequest;
import com.teo1.clinicadental.dto.HistorialClinicoResponse;
import com.teo1.clinicadental.dto.ListaNegraRequest;
import com.teo1.clinicadental.dto.RegistroRequest;
import com.teo1.clinicadental.service.ClienteService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@RestController
@RequestMapping("/pacientes")
@RequiredArgsConstructor
@Tag(name = "Clientes", description = "Gestión de pacientes, lista negra e historial clínico")
@SecurityRequirement(name = "bearerAuth")
public class ClienteController {

    private final ClienteService clienteService;

    @GetMapping
    @Operation(summary = "Listar pacientes")
    @ApiResponse(responseCode = "200", description = "Lista de pacientes")
    @ApiResponse(responseCode = "401", description = "Falta autenticación válida")
    @ApiResponse(responseCode = "403", description = "Rol sin permiso para esta operación")
    public ResponseEntity<List<ClienteResponse>> listarPacientes() {
        return ResponseEntity.ok(clienteService.listarPacientes());
    }

    @PostMapping
    @Operation(summary = "Crear un paciente")
    @ApiResponse(responseCode = "201", description = "Paciente creado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos")
    @ApiResponse(responseCode = "401", description = "Falta autenticación válida")
    @ApiResponse(responseCode = "403", description = "Rol sin permiso para esta operación")
    @ApiResponse(responseCode = "409", description = "Conflicto con una restricción de datos")
    public ResponseEntity<ClienteResponse> crearPaciente(@Valid @RequestBody RegistroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(clienteService.crearPaciente(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un paciente")
    @ApiResponse(responseCode = "200", description = "Paciente encontrado")
    @ApiResponse(responseCode = "400", description = "Identificador no válido")
    @ApiResponse(responseCode = "401", description = "Falta autenticación válida")
    @ApiResponse(responseCode = "403", description = "Rol sin permiso para esta operación")
    @ApiResponse(responseCode = "404", description = "Paciente no encontrado")
    public ResponseEntity<ClienteResponse> obtenerPaciente(@PathVariable UUID id) {
        return ResponseEntity.ok(clienteService.obtenerPaciente(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un paciente")
    @ApiResponse(responseCode = "200", description = "Paciente actualizado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos o identificador no válido")
    @ApiResponse(responseCode = "401", description = "Falta autenticación válida")
    @ApiResponse(responseCode = "403", description = "Rol sin permiso para esta operación")
    @ApiResponse(responseCode = "404", description = "Paciente no encontrado")
    @ApiResponse(responseCode = "409", description = "Conflicto con una restricción de datos")
    public ResponseEntity<ClienteResponse> actualizarPaciente(
            @PathVariable UUID id,
            @Valid @RequestBody ClienteUpdateRequest request
    ) {
        return ResponseEntity.ok(clienteService.actualizarPaciente(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Desactivar un paciente")
    @ApiResponse(responseCode = "204", description = "Paciente desactivado")
    @ApiResponse(responseCode = "400", description = "Identificador no válido")
    @ApiResponse(responseCode = "401", description = "Falta autenticación válida")
    @ApiResponse(responseCode = "403", description = "Rol sin permiso para esta operación")
    @ApiResponse(responseCode = "404", description = "Paciente no encontrado")
    public ResponseEntity<Void> desactivarPaciente(@PathVariable UUID id) {
        clienteService.desactivarPaciente(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/lista-negra")
    @Operation(summary = "Actualizar la condición de lista negra")
    @ApiResponse(responseCode = "200", description = "Condición actualizada")
    @ApiResponse(responseCode = "400", description = "Datos inválidos o identificador no válido")
    @ApiResponse(responseCode = "401", description = "Falta autenticación válida")
    @ApiResponse(responseCode = "403", description = "Se requiere el rol SECRETARIA")
    @ApiResponse(responseCode = "404", description = "Paciente no encontrado")
    public ResponseEntity<ClienteResponse> actualizarListaNegra(
            @PathVariable UUID id,
            @Valid @RequestBody ListaNegraRequest request
    ) {
        return ResponseEntity.ok(clienteService.actualizarListaNegra(id, request));
    }

    @GetMapping("/{id}/historial")
    @Operation(summary = "Listar el historial clínico")
    @ApiResponse(responseCode = "200", description = "Historial clínico del paciente")
    @ApiResponse(responseCode = "400", description = "Identificador no válido")
    @ApiResponse(responseCode = "401", description = "Falta autenticación válida")
    @ApiResponse(responseCode = "403", description = "Rol sin permiso para esta operación")
    @ApiResponse(responseCode = "404", description = "Paciente no encontrado")
    public ResponseEntity<List<HistorialClinicoResponse>> listarHistorial(@PathVariable UUID id) {
        return ResponseEntity.ok(clienteService.listarHistorial(id));
    }

    @PostMapping("/{id}/historial")
    @Operation(summary = "Agregar una entrada al historial clínico")
    @ApiResponse(responseCode = "201", description = "Entrada agregada")
    @ApiResponse(responseCode = "400", description = "Datos inválidos")
    @ApiResponse(responseCode = "401", description = "Falta autenticación válida")
    @ApiResponse(responseCode = "403", description = "Se requiere el rol ADMIN o DOCTOR")
    @ApiResponse(responseCode = "404", description = "Paciente no encontrado")
    @ApiResponse(responseCode = "409", description = "Conflicto con una restricción de datos")
    public ResponseEntity<HistorialClinicoResponse> agregarHistorial(
            @PathVariable UUID id,
            @Valid @RequestBody HistorialClinicoRequest request
    ) {
        HistorialClinicoResponse response = clienteService.agregarHistorial(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
