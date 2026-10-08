package com.teo1.clinicadental.controller;

import com.teo1.clinicadental.dto.CitaRequest;
import com.teo1.clinicadental.dto.CitaResponse;
import com.teo1.clinicadental.dto.ErrorResponse;
import com.teo1.clinicadental.model.EstadoCita;
import com.teo1.clinicadental.service.CitaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/citas")
@RequiredArgsConstructor
@Tag(name = "Citas", description = "Agendamiento y consulta de citas")
@SecurityRequirement(name = "bearerAuth")
public class CitaController {

    private final CitaService citaService;

    @PostMapping
    @Operation(
            summary = "Agendar una cita",
            description = "Requiere rol CLIENTE, SECRETARIA o ADMIN. Un CLIENTE agenda para si mismo y se ignora idCliente; "
                    + "ADMIN y SECRETARIA deben indicar idCliente. El doctor y el paciente deben estar activos y el paciente "
                    + "no puede estar en lista negra. La fecha y hora deben ser futuras, la cita completa (duracion configurada "
                    + "en app.citas.duracion-minutos) debe caber en un horario del doctor para ese dia y no puede solaparse "
                    + "con otra cita no cancelada del mismo doctor; dos citas que solo se tocan en el borde si se permiten."
    )
    @ApiResponse(responseCode = "201", description = "Cita agendada")
    @ApiResponse(responseCode = "400", description = "Datos invalidos, falta idCliente o la fecha y hora ya pasaron", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Falta autenticacion valida", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Rol sin permiso para agendar o paciente en lista negra", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Doctor o paciente no encontrado", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Doctor o paciente inactivo, cita fuera del horario del doctor o solapada con otra cita", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<CitaResponse> crearCita(
            @Valid @RequestBody CitaRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(citaService.crearCita(request, authentication));
    }

    @GetMapping
    @Operation(
            summary = "Listar citas",
            description = "Requiere sesion. CLIENTE y DOCTOR ven solo sus citas; SECRETARIA y ADMIN ven todas. "
                    + "Los filtros son opcionales y se combinan. Ordenadas por fecha y hora ascendente."
    )
    @ApiResponse(responseCode = "200", description = "Lista de citas")
    @ApiResponse(responseCode = "400", description = "Filtro con formato invalido", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Falta autenticacion valida", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<List<CitaResponse>> listarCitas(
            @Parameter(description = "Fecha en formato yyyy-MM-dd", example = "2026-10-15")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @Parameter(description = "Identificador del doctor")
            @RequestParam(required = false) UUID idDoctor,
            @Parameter(description = "Identificador del paciente")
            @RequestParam(required = false) UUID idCliente,
            @Parameter(description = "Estado de la cita", example = "AGENDADA")
            @RequestParam(required = false) EstadoCita estado,
            Authentication authentication
    ) {
        return ResponseEntity.ok(citaService.listarCitas(fecha, idDoctor, idCliente, estado, authentication));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Obtener una cita",
            description = "Requiere sesion. CLIENTE y DOCTOR solo pueden ver sus propias citas; SECRETARIA y ADMIN ven cualquiera."
    )
    @ApiResponse(responseCode = "200", description = "Cita encontrada")
    @ApiResponse(responseCode = "400", description = "Identificador no valido", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Falta autenticacion valida", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "La cita pertenece a otra persona", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Cita no encontrada", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<CitaResponse> obtenerCita(@PathVariable UUID id, Authentication authentication) {
        return ResponseEntity.ok(citaService.obtenerCita(id, authentication));
    }
}
