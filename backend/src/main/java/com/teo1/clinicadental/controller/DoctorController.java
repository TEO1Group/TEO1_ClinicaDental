package com.teo1.clinicadental.controller;

import com.teo1.clinicadental.dto.DoctorResponse;
import com.teo1.clinicadental.dto.DoctorUpdateRequest;
import com.teo1.clinicadental.dto.HorarioRequest;
import com.teo1.clinicadental.dto.HorarioResponse;
import com.teo1.clinicadental.service.DoctorService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
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
@RequestMapping("/doctores")
@RequiredArgsConstructor
@Tag(name = "Doctores", description = "Consulta de doctores y administración de horarios")
@SecurityRequirement(name = "bearerAuth")
public class DoctorController {

    private final DoctorService doctorService;

    @GetMapping
    @Operation(summary = "Listar doctores")
    @ApiResponse(responseCode = "200", description = "Lista de doctores")
    @ApiResponse(responseCode = "401", description = "Falta autenticación válida")
    public ResponseEntity<List<DoctorResponse>> listarDoctores() {
        return ResponseEntity.ok(doctorService.listarDoctores());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un doctor")
    @ApiResponse(responseCode = "200", description = "Doctor encontrado")
    @ApiResponse(responseCode = "400", description = "Identificador no válido")
    @ApiResponse(responseCode = "401", description = "Falta autenticación válida")
    @ApiResponse(responseCode = "404", description = "Doctor no encontrado")
    public ResponseEntity<DoctorResponse> obtenerDoctor(@PathVariable UUID id) {
        return ResponseEntity.ok(doctorService.obtenerDoctor(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar un doctor")
    @ApiResponse(responseCode = "200", description = "Doctor actualizado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos o identificador no válido")
    @ApiResponse(responseCode = "401", description = "Falta autenticación válida")
    @ApiResponse(responseCode = "403", description = "Se requiere el rol ADMIN")
    @ApiResponse(responseCode = "404", description = "Doctor no encontrado")
    @ApiResponse(responseCode = "409", description = "Conflicto con una restricción de datos")
    public ResponseEntity<DoctorResponse> actualizarDoctor(
            @PathVariable UUID id,
            @Valid @RequestBody DoctorUpdateRequest request
    ) {
        return ResponseEntity.ok(doctorService.actualizarDoctor(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Desactivar un doctor")
    @ApiResponse(responseCode = "204", description = "Doctor desactivado")
    @ApiResponse(responseCode = "400", description = "Identificador no válido")
    @ApiResponse(responseCode = "401", description = "Falta autenticación válida")
    @ApiResponse(responseCode = "403", description = "Se requiere el rol ADMIN")
    @ApiResponse(responseCode = "404", description = "Doctor no encontrado")
    public ResponseEntity<Void> desactivarDoctor(@PathVariable UUID id) {
        doctorService.desactivarDoctor(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/horarios")
    @Operation(summary = "Listar horarios de un doctor")
    @ApiResponse(responseCode = "200", description = "Lista de horarios")
    @ApiResponse(responseCode = "400", description = "Identificador no válido")
    @ApiResponse(responseCode = "401", description = "Falta autenticación válida")
    @ApiResponse(responseCode = "404", description = "Doctor no encontrado")
    public ResponseEntity<List<HorarioResponse>> listarHorarios(@PathVariable UUID id) {
        return ResponseEntity.ok(doctorService.listarHorarios(id));
    }

    @PostMapping("/{id}/horarios")
    @Operation(summary = "Agregar un horario a un doctor")
    @ApiResponse(responseCode = "201", description = "Horario creado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos")
    @ApiResponse(responseCode = "401", description = "Falta autenticación válida")
    @ApiResponse(responseCode = "403", description = "Rol sin permiso para esta operación")
    @ApiResponse(responseCode = "404", description = "Doctor no encontrado")
    @ApiResponse(responseCode = "409", description = "Conflicto con una restricción de datos")
    public ResponseEntity<HorarioResponse> agregarHorario(
            @PathVariable UUID id,
            @Valid @RequestBody HorarioRequest request,
            Authentication authentication
    ) {
        HorarioResponse response = doctorService.agregarHorario(id, request, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}/horarios/{idHorario}")
    @Operation(summary = "Actualizar un horario")
    @ApiResponse(responseCode = "200", description = "Horario actualizado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos o identificador no válido")
    @ApiResponse(responseCode = "401", description = "Falta autenticación válida")
    @ApiResponse(responseCode = "403", description = "Rol sin permiso para esta operación")
    @ApiResponse(responseCode = "404", description = "Doctor u horario no encontrado")
    @ApiResponse(responseCode = "409", description = "Conflicto con una restricción de datos")
    public ResponseEntity<HorarioResponse> actualizarHorario(
            @PathVariable UUID id,
            @PathVariable UUID idHorario,
            @Valid @RequestBody HorarioRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(doctorService.actualizarHorario(id, idHorario, request, authentication));
    }

    @DeleteMapping("/{id}/horarios/{idHorario}")
    @Operation(summary = "Eliminar un horario")
    @ApiResponse(responseCode = "204", description = "Horario eliminado")
    @ApiResponse(responseCode = "400", description = "Identificador no válido")
    @ApiResponse(responseCode = "401", description = "Falta autenticación válida")
    @ApiResponse(responseCode = "403", description = "Rol sin permiso para esta operación")
    @ApiResponse(responseCode = "404", description = "Doctor u horario no encontrado")
    public ResponseEntity<Void> eliminarHorario(
            @PathVariable UUID id,
            @PathVariable UUID idHorario,
            Authentication authentication
    ) {
        doctorService.eliminarHorario(id, idHorario, authentication);
        return ResponseEntity.noContent().build();
    }
}
