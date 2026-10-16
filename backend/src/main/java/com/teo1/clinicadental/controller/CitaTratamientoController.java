package com.teo1.clinicadental.controller;

import com.teo1.clinicadental.dto.CitaTratamientoRequest;
import com.teo1.clinicadental.dto.CitaTratamientoResponse;
import com.teo1.clinicadental.dto.ErrorResponse;
import com.teo1.clinicadental.dto.TratamientosPacienteResponse;
import com.teo1.clinicadental.service.TratamientoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Tratamientos", description = "Catalogo de tratamientos y tratamientos realizados en las citas")
@SecurityRequirement(name = "bearerAuth")
public class CitaTratamientoController {

    private final TratamientoService tratamientoService;

    @PostMapping("/citas/{idCita}/tratamientos")
    @Operation(
            summary = "Registrar un tratamiento en una cita",
            description = "Requiere rol DOCTOR y solo el doctor de esa cita puede registrar; otro doctor, ADMIN, SECRETARIA "
                    + "y CLIENTE no. La cita debe estar ATENDIDA. Si no se envia costo se usa el del catalogo; si se envia "
                    + "no puede ser negativo. Se puede registrar el mismo tratamiento mas de una vez en la misma cita."
    )
    @ApiResponse(responseCode = "201", description = "Tratamiento registrado")
    @ApiResponse(responseCode = "400", description = "Falta idTratamiento, costo negativo o con formato invalido, observaciones demasiado largas o identificador no valido", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Falta autenticacion valida", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "El usuario no es el doctor de la cita", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Cita o tratamiento no encontrado", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "La cita no esta ATENDIDA", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<CitaTratamientoResponse> registrar(
            @PathVariable UUID idCita,
            @Valid @RequestBody CitaTratamientoRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tratamientoService.registrar(idCita, request, authentication));
    }

    @GetMapping("/citas/{idCita}/tratamientos")
    @Operation(
            summary = "Listar los tratamientos de una cita",
            description = "Requiere rol ADMIN, SECRETARIA o el DOCTOR de esa cita; otro doctor y el CLIENTE no. "
                    + "Ordenados por fecha de registro."
    )
    @ApiResponse(responseCode = "200", description = "Tratamientos de la cita")
    @ApiResponse(responseCode = "400", description = "Identificador no valido", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Falta autenticacion valida", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "El usuario es CLIENTE o es un DOCTOR que no atendio la cita", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Cita no encontrada", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<List<CitaTratamientoResponse>> listarPorCita(
            @PathVariable UUID idCita,
            Authentication authentication
    ) {
        return ResponseEntity.ok(tratamientoService.listarPorCita(idCita, authentication));
    }

    @GetMapping("/pacientes/{idCliente}/tratamientos")
    @Operation(
            summary = "Listar los tratamientos de un paciente",
            description = "Requiere rol ADMIN, DOCTOR o SECRETARIA, igual que el historial clinico. Devuelve todos los "
                    + "tratamientos registrados en las citas del paciente, de la cita mas reciente a la mas antigua, y el "
                    + "total de los costos cobrados (0 si no tiene ninguno)."
    )
    @ApiResponse(responseCode = "200", description = "Tratamientos del paciente y total")
    @ApiResponse(responseCode = "400", description = "Identificador no valido", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Falta autenticacion valida", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Se requiere el rol ADMIN, DOCTOR o SECRETARIA", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Paciente no encontrado", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<TratamientosPacienteResponse> listarPorPaciente(@PathVariable UUID idCliente) {
        return ResponseEntity.ok(tratamientoService.listarPorPaciente(idCliente));
    }

    @GetMapping("/mis-tratamientos")
    @Operation(
            summary = "Listar mis tratamientos",
            description = "Requiere rol CLIENTE; ADMIN, DOCTOR y SECRETARIA no. Solo lectura. El paciente se obtiene del "
                    + "usuario autenticado, por eso no recibe ningun identificador. Devuelve los tratamientos registrados "
                    + "en las citas del paciente, de la cita mas reciente a la mas antigua, y el total de los costos "
                    + "cobrados (0 si no tiene ninguno)."
    )
    @ApiResponse(responseCode = "200", description = "Tratamientos del paciente autenticado y total")
    @ApiResponse(responseCode = "401", description = "Falta autenticacion valida", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Se requiere el rol CLIENTE", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "El usuario no tiene un perfil de paciente", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<TratamientosPacienteResponse> listarMisTratamientos(Authentication authentication) {
        return ResponseEntity.ok(tratamientoService.listarMisTratamientos(authentication));
    }
}
