package com.teo1.clinicadental.controller;

import com.teo1.clinicadental.dto.CitaRequest;
import com.teo1.clinicadental.dto.CitaResponse;
import com.teo1.clinicadental.dto.ErrorResponse;
import com.teo1.clinicadental.dto.EstadoCitaRequest;
import com.teo1.clinicadental.dto.ReprogramarCitaRequest;
import com.teo1.clinicadental.model.EstadoCita;
import com.teo1.clinicadental.service.CitaService;
import com.teo1.clinicadental.service.ReprogramacionCitaService;
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
import org.springframework.web.bind.annotation.PatchMapping;
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
    private final ReprogramacionCitaService reprogramacionCitaService;

    @PostMapping
    @Operation(
            summary = "Agendar una cita",
            description = "Requiere rol CLIENTE, SECRETARIA o ADMIN. Un CLIENTE agenda para si mismo y se ignora idCliente; "
                    + "ADMIN y SECRETARIA deben indicar idCliente. El doctor y el paciente deben estar activos y el paciente "
                    + "no puede estar en lista negra. La fecha y hora deben ser futuras, la cita completa (duracion configurada "
                    + "en app.citas.duracion-minutos) debe caber en un horario del doctor para ese dia y no puede solaparse "
                    + "con otra cita no cancelada del mismo doctor; dos citas que solo se tocan en el borde si se permiten. "
                    + "Tampoco puede cruzarse con un bloqueo del doctor. Si el paciente alcanza app.citas.max-inasistencias "
                    + "(3 por defecto) desde su ultimo levantamiento, se rechaza con 409 hasta levantar la restriccion. "
                    + "Al agendar se envia en ese momento el correo de recordatorio al paciente, si SMTP esta configurado."
    )
    @ApiResponse(responseCode = "201", description = "Cita agendada")
    @ApiResponse(responseCode = "400", description = "Datos invalidos, falta idCliente o la fecha y hora ya pasaron", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Falta autenticacion valida", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Rol sin permiso para agendar o paciente en lista negra", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Doctor o paciente no encontrado", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Limite de inasistencias alcanzado, doctor o paciente inactivo, cita fuera del horario del doctor, dentro de un bloqueo del doctor o solapada con otra cita", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
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

    @GetMapping("/proximas")
    @Operation(
            summary = "Listar citas proximas",
            description = "Requiere sesion. Devuelve las citas AGENDADA desde ahora hasta dentro de 'horas', "
                    + "de la mas cercana a la mas lejana. CLIENTE y DOCTOR reciben solo las suyas; SECRETARIA y ADMIN reciben "
                    + "siempre una lista vacia."
    )
    @ApiResponse(responseCode = "200", description = "Lista de citas proximas")
    @ApiResponse(responseCode = "400", description = "Horas fuera del rango 1 a 72", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Falta autenticacion valida", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<List<CitaResponse>> listarProximas(
            @Parameter(description = "Ventana en horas desde ahora (1 a 72)", example = "48")
            @RequestParam(defaultValue = "48") int horas,
            Authentication authentication
    ) {
        return ResponseEntity.ok(citaService.listarProximas(horas, authentication));
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

    @PatchMapping("/{id}/estado")
    @Operation(
            summary = "Cambiar el estado de una cita",
            description = "Solo una cita AGENDADA puede cambiar; ATENDIDA, CANCELADA y NO_ASISTIO son finales. "
                    + "CANCELADA la pueden hacer el CLIENTE (solo su cita), SECRETARIA y ADMIN; el DOCTOR no. "
                    + "ATENDIDA y NO_ASISTIO las pueden hacer el DOCTOR (solo sus citas), SECRETARIA y ADMIN; el CLIENTE no. "
                    + "CANCELADA y NO_ASISTIO exigen motivo (maximo 200 caracteres); en ATENDIDA es opcional. El motivo se "
                    + "devuelve en el campo motivo de la respuesta. El CLIENTE puede cancelar su cita en cualquier momento, "
                    + "incluso el mismo dia. El DOCTOR solo "
                    + "puede marcar ATENDIDA o NO_ASISTIO desde la hora de inicio de la cita. SECRETARIA y ADMIN no tienen "
                    + "limites de tiempo. Cancelar una cita libera el horario para volver a agendarlo."
    )
    @ApiResponse(responseCode = "200", description = "Estado actualizado")
    @ApiResponse(responseCode = "400", description = "Estado invalido, se pidio AGENDADA, falta el motivo para CANCELADA o NO_ASISTIO, o el motivo supera 200 caracteres", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Falta autenticacion valida", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Rol sin permiso para ese estado o la cita es de otra persona", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Cita no encontrada", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "La cita ya esta en un estado final o el DOCTOR marca la cita antes de su hora de inicio", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<CitaResponse> cambiarEstado(
            @PathVariable UUID id,
            @Valid @RequestBody EstadoCitaRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(citaService.cambiarEstado(id, request, authentication));
    }

    @PatchMapping("/{id}/reprogramar")
    @Operation(
            summary = "Reprogramar una cita",
            description = "Cambia la fecha y hora de una cita AGENDADA; conserva su doctor, su paciente y su estado. "
                    + "La pueden reprogramar el CLIENTE (solo su cita), SECRETARIA y ADMIN; el DOCTOR no. El motivo es "
                    + "obligatorio para todos (maximo 200 caracteres) y queda en el historial de reprogramaciones, no en el "
                    + "campo motivo de la cita. Ningun rol tiene plazo minimo de anticipacion: "
                    + "se puede reprogramar el mismo dia. La nueva fecha y hora deben ser futuras, distintas de las actuales, "
                    + "caber en un horario del doctor, no cruzarse con un bloqueo del doctor y no solaparse con otra cita "
                    + "(sin contar la propia). La hora anterior queda libre y el correo de recordatorio con la nueva fecha "
                    + "se envia al paciente en ese momento, si SMTP esta configurado."
    )
    @ApiResponse(responseCode = "200", description = "Cita reprogramada")
    @ApiResponse(responseCode = "400", description = "Falta fecha, hora o motivo, el motivo supera 200 caracteres, la nueva fecha y hora son iguales a las actuales o ya pasaron", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Falta autenticacion valida", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "El usuario es DOCTOR o es un CLIENTE y la cita es de otra persona", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Cita no encontrada", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "La cita no esta AGENDADA o la nueva fecha y hora quedan fuera del horario del doctor, dentro de un bloqueo o solapadas con otra cita", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<CitaResponse> reprogramar(
            @PathVariable UUID id,
            @Valid @RequestBody ReprogramarCitaRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(reprogramacionCitaService.reprogramar(id, request, authentication));
    }
}
