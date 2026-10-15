package com.teo1.clinicadental.controller;

import com.teo1.clinicadental.dto.BloqueoDoctorRequest;
import com.teo1.clinicadental.dto.BloqueoDoctorResponse;
import com.teo1.clinicadental.dto.ErrorResponse;
import com.teo1.clinicadental.service.BloqueoDoctorService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/doctores/{idDoctor}/bloqueos")
@RequiredArgsConstructor
@Tag(name = "Bloqueos de doctor", description = "Periodos en los que un doctor no atiende")
@SecurityRequirement(name = "bearerAuth")
public class BloqueoDoctorController {

    private final BloqueoDoctorService bloqueoDoctorService;

    @PostMapping
    @Operation(
            summary = "Crear un bloqueo",
            description = "Requiere rol ADMIN o SECRETARIA. Inicio y fin son fecha y hora de Guatemala y el inicio debe ser "
                    + "anterior al fin. No se puede crear si el doctor tiene citas AGENDADA cuyo intervalo (hora de inicio "
                    + "mas la duracion de la cita) se cruza con el periodo: primero deben reprogramarse o cancelarse. "
                    + "Mientras exista el bloqueo no se pueden agendar ni reprogramar citas que se crucen con el; una cita "
                    + "que termina justo cuando empieza el bloqueo, o empieza justo cuando termina, si se permite."
    )
    @ApiResponse(responseCode = "201", description = "Bloqueo creado")
    @ApiResponse(responseCode = "400", description = "Falta inicio, fin o motivo, el motivo supera 200 caracteres, el inicio no es anterior al fin o el identificador no es valido", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Falta autenticacion valida", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Se requiere el rol ADMIN o SECRETARIA", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Doctor no encontrado", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "El doctor tiene citas agendadas en ese periodo", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<BloqueoDoctorResponse> crearBloqueo(
            @PathVariable UUID idDoctor,
            @Valid @RequestBody BloqueoDoctorRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(bloqueoDoctorService.crearBloqueo(idDoctor, request, authentication));
    }

    @GetMapping
    @Operation(
            summary = "Listar los bloqueos de un doctor",
            description = "Requiere rol ADMIN, SECRETARIA o DOCTOR. El DOCTOR solo puede ver sus propios bloqueos; "
                    + "el CLIENTE no puede verlos. Ordenados por inicio ascendente."
    )
    @ApiResponse(responseCode = "200", description = "Lista de bloqueos")
    @ApiResponse(responseCode = "400", description = "Identificador no valido", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Falta autenticacion valida", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "El usuario es CLIENTE o es un DOCTOR que pide los bloqueos de otro doctor", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Doctor no encontrado", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<List<BloqueoDoctorResponse>> listarBloqueos(
            @PathVariable UUID idDoctor,
            Authentication authentication
    ) {
        return ResponseEntity.ok(bloqueoDoctorService.listarBloqueos(idDoctor, authentication));
    }

    @DeleteMapping("/{idBloqueo}")
    @Operation(
            summary = "Eliminar un bloqueo",
            description = "Requiere rol ADMIN o SECRETARIA. Al eliminarlo el periodo vuelve a quedar disponible para agendar."
    )
    @ApiResponse(responseCode = "204", description = "Bloqueo eliminado", content = @Content)
    @ApiResponse(responseCode = "400", description = "Identificador no valido", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Falta autenticacion valida", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Se requiere el rol ADMIN o SECRETARIA", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "El bloqueo no existe o no pertenece a ese doctor", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<Void> eliminarBloqueo(@PathVariable UUID idDoctor, @PathVariable UUID idBloqueo) {
        bloqueoDoctorService.eliminarBloqueo(idDoctor, idBloqueo);
        return ResponseEntity.noContent().build();
    }
}
