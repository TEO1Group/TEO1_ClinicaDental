package com.teo1.clinicadental.controller;

import com.teo1.clinicadental.dto.ErrorResponse;
import com.teo1.clinicadental.dto.InasistenciasPacienteResponse;
import com.teo1.clinicadental.dto.LevantamientoInasistenciaRequest;
import com.teo1.clinicadental.service.InasistenciaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/pacientes/{id}/inasistencias")
@RequiredArgsConstructor
@Tag(name = "Inasistencias", description = "Restriccion de agendado por inasistencias y su levantamiento")
@SecurityRequirement(name = "bearerAuth")
public class InasistenciaController {

    private final InasistenciaService inasistenciaService;

    @GetMapping
    @Operation(summary = "Consultar las inasistencias de un paciente", description = "Requiere rol ADMIN, DOCTOR o SECRETARIA. Devuelve las citas NO_ASISTIO desde el ultimo levantamiento, el limite configurado (app.citas.max-inasistencias) y si el paciente alcanzo ese limite. El corte usa la fecha y hora de las citas, no cuando se marcaron. Esta restriccion es independiente de la lista negra.")
    @ApiResponse(responseCode = "200", description = "Estado de inasistencias del paciente")
    @ApiResponse(responseCode = "400", description = "Identificador no válido", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Falta autenticación válida", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Rol sin permiso para esta operación", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Paciente no encontrado", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<InasistenciasPacienteResponse> obtenerEstado(@PathVariable UUID id) {
        return ResponseEntity.ok(inasistenciaService.obtenerEstado(id));
    }

    @PostMapping("/levantamientos")
    @Operation(summary = "Levantar la restriccion por inasistencias", description = "Requiere rol ADMIN o SECRETARIA. El motivo es obligatorio; se guarda quien, cuando y por que. Requiere haber alcanzado el limite configurado (3 por defecto); de lo contrario devuelve 409. Desde ese momento el conteo usa solo citas posteriores al levantamiento. No borra citas ni levanta la lista negra.")
    @ApiResponse(responseCode = "201", description = "Restriccion levantada; devuelve el estado actualizado")
    @ApiResponse(responseCode = "400", description = "Motivo vacío, de más de 200 caracteres o identificador no válido", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Falta autenticación válida", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Se requiere el rol ADMIN o SECRETARIA", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Paciente no encontrado", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "El paciente no tiene la restriccion por inasistencias", content = @Content(
            mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class),
            examples = @ExampleObject(name = "sinRestriccion", summary = "Ejemplo con limite predeterminado de 3", value = """
                    {"status":409,"error":"Conflict","mensaje":"El paciente no tiene la restriccion por inasistencias (0 de 3)","path":"/pacientes/8a2b3c4d-5e6f-4a1b-8c2d-3e4f5a6b7c8d/inasistencias/levantamientos"}
                    """)))
    public ResponseEntity<InasistenciasPacienteResponse> levantarRestriccion(
            @PathVariable UUID id,
            @Valid @RequestBody LevantamientoInasistenciaRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(inasistenciaService.levantarRestriccion(id, request, authentication));
    }
}
