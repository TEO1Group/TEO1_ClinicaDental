package com.teo1.clinicadental.controller;

import com.teo1.clinicadental.dto.AbonoPlanRequest;
import com.teo1.clinicadental.dto.AbonoPlanResponse;
import com.teo1.clinicadental.dto.AbonosPlanResponse;
import com.teo1.clinicadental.dto.CitaResponse;
import com.teo1.clinicadental.dto.ErrorResponse;
import com.teo1.clinicadental.dto.PlanTratamientoRequest;
import com.teo1.clinicadental.dto.PlanTratamientoResponse;
import com.teo1.clinicadental.dto.SesionPlanRequest;
import com.teo1.clinicadental.model.EstadoPlan;
import com.teo1.clinicadental.service.PlanTratamientoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/planes")
@RequiredArgsConstructor
@Tag(name = "Planes de tratamiento", description = "Planes de varias sesiones con citas vinculadas, avance y saldo")
@SecurityRequirement(name = "bearerAuth")
public class PlanTratamientoController {

    private final PlanTratamientoService planTratamientoService;

    @PostMapping
    @Operation(summary = "Crear un plan de tratamiento", description = "Requiere rol ADMIN, SECRETARIA o DOCTOR. ADMIN y SECRETARIA indican idDoctor; un DOCTOR crea planes solo para si mismo. El paciente y el doctor deben estar activos y el tratamiento activo en el catalogo. Si se omite costoTotal se usa el costo del catalogo por el numero de sesiones.")
    @ApiResponse(responseCode = "201", description = "Plan creado")
    @ApiResponse(responseCode = "400", description = "Datos invalidos o falta idDoctor", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Falta autenticacion valida", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Rol sin permiso o un DOCTOR indica otro doctor", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Paciente, doctor o tratamiento no encontrado", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Paciente o doctor inactivo, o tratamiento inactivo", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<PlanTratamientoResponse> crearPlan(
            @Valid @RequestBody PlanTratamientoRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(planTratamientoService.crearPlan(request, authentication));
    }

    @GetMapping
    @Operation(summary = "Listar planes de tratamiento", description = "Requiere rol ADMIN, SECRETARIA o DOCTOR. Un DOCTOR solo ve sus planes. Cada plan incluye avance y saldo, sin las citas. Ordenados del mas reciente al mas antiguo.")
    @ApiResponse(responseCode = "200", description = "Lista de planes")
    @ApiResponse(responseCode = "401", description = "Falta autenticacion valida", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Rol sin permiso para esta operacion", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<List<PlanTratamientoResponse>> listarPlanes(
            @Parameter(description = "Identificador del paciente")
            @RequestParam(required = false) UUID idCliente,
            @Parameter(description = "Identificador del doctor")
            @RequestParam(required = false) UUID idDoctor,
            @Parameter(description = "Estado del plan", example = "ACTIVO")
            @RequestParam(required = false) EstadoPlan estado,
            Authentication authentication
    ) {
        return ResponseEntity.ok(planTratamientoService.listarPlanes(idCliente, idDoctor, estado, authentication));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un plan con su avance, saldo y citas", description = "Requiere rol ADMIN, SECRETARIA o DOCTOR; un DOCTOR solo ve sus planes. El avance es sesiones atendidas sobre totales y el saldo es el costo total menos los abonos.")
    @ApiResponse(responseCode = "200", description = "Plan encontrado")
    @ApiResponse(responseCode = "400", description = "Identificador no valido", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Falta autenticacion valida", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Rol sin permiso o plan de otro doctor", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Plan no encontrado", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<PlanTratamientoResponse> obtenerPlan(@PathVariable UUID id, Authentication authentication) {
        return ResponseEntity.ok(planTratamientoService.obtenerPlan(id, authentication));
    }

    @PostMapping("/{id}/citas")
    @Operation(summary = "Agendar una sesion del plan", description = "Requiere rol ADMIN o SECRETARIA. Crea la cita con el paciente y el doctor del plan, con las mismas reglas que POST /citas (horario, solapes, bloqueos del doctor, lista negra e inasistencias), y la vincula al plan. Una sesion cuenta mientras su cita esta AGENDADA o ATENDIDA; si se cancela o el paciente no asiste se puede agendar otra.")
    @ApiResponse(responseCode = "201", description = "Sesion agendada")
    @ApiResponse(responseCode = "400", description = "Datos invalidos o la fecha y hora ya pasaron", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Falta autenticacion valida", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Rol sin permiso o paciente en lista negra", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Plan no encontrado", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Ya se agendaron todas las sesiones, el plan esta completado o cancelado, el paciente esta bloqueado por inasistencias, o el horario no esta disponible", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<CitaResponse> agendarSesion(
            @PathVariable UUID id,
            @Valid @RequestBody SesionPlanRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(planTratamientoService.agendarSesion(id, request, authentication));
    }
    @PostMapping("/{id}/abonos")
    @Operation(summary = "Registrar un abono al plan", description = "Solo ADMIN y SECRETARIA. Monto obligatorio, positivo, de hasta 8 enteros y 2 decimales; puede ser igual al saldo pero no superarlo. Impide sobrepagos incluso ante abonos simultaneos. Fecha en hora de Guatemala y registradoPor se obtienen del servidor/JWT; no se reciben del cliente. No depende del estado del plan ni modifica su avance.")
    @ApiResponse(responseCode = "201", description = "Abono registrado")
    @ApiResponse(responseCode = "400", description = "Monto ausente, no positivo, fuera de precision, cuerpo o identificador invalido", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Falta autenticacion valida", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Solo ADMIN y SECRETARIA", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Plan no encontrado", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "El monto supera el saldo, incluso si otro abono acaba de reducirlo", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<AbonoPlanResponse> registrarAbono(@PathVariable UUID id,
            @Valid @RequestBody AbonoPlanRequest request, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(planTratamientoService.registrarAbono(id, request, authentication));
    }

    @GetMapping("/{id}/abonos")
    @Operation(summary = "Consultar abonos y saldo del plan", description = "ADMIN y SECRETARIA consultan cualquier plan; DOCTOR solo los suyos. CLIENTE no accede a esta ruta. Devuelve costo total, total abonado, saldo y lista de abonos por fecha/identificador (vacia si no hay). La lista y el saldo corresponden a una misma instantanea, incluso con abonos concurrentes.")
    @ApiResponse(responseCode = "200", description = "Abonos y saldo del plan")
    @ApiResponse(responseCode = "400", description = "Identificador invalido", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Falta autenticacion valida", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Rol sin permiso o plan de otro doctor", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Plan no encontrado", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<AbonosPlanResponse> listarAbonos(@PathVariable UUID id, Authentication authentication) {
        return ResponseEntity.ok(planTratamientoService.listarAbonos(id, authentication));
    }

}
