package com.teo1.clinicadental.controller;

import com.teo1.clinicadental.dto.BloqueoVigenteResponse;
import com.teo1.clinicadental.dto.ErrorResponse;
import com.teo1.clinicadental.service.BloqueoDoctorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Bloqueos de doctor", description = "Periodos en los que un doctor no atiende")
@SecurityRequirement(name = "bearerAuth")
public class BloqueoVigenteController {

    private final BloqueoDoctorService bloqueoDoctorService;

    @GetMapping("/doctores/{idDoctor}/bloqueos-vigentes")
    @Operation(
            summary = "Listar los bloqueos vigentes de un doctor",
            description = "Requiere sesion iniciada con cualquier rol, incluido CLIENTE. Solo lectura. Devuelve inicio, fin "
                    + "y motivo de los bloqueos que aun no terminan (fin posterior a la hora actual de Guatemala), "
                    + "incluido el que esta en curso, ordenados por inicio ascendente; lista vacia si no hay. "
                    + "Sirve para avisar que el doctor no esta disponible antes de agendar o reprogramar."
    )
    @ApiResponse(responseCode = "200", description = "Bloqueos vigentes del doctor")
    @ApiResponse(responseCode = "400", description = "Identificador no valido", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Falta autenticacion valida", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "Doctor no encontrado", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<List<BloqueoVigenteResponse>> listarBloqueosVigentes(
            @Parameter(description = "Identificador del doctor", example = "1d2e3f4a-5b6c-4d7e-8f9a-0b1c2d3e4f5a")
            @PathVariable UUID idDoctor) {
        return ResponseEntity.ok(bloqueoDoctorService.listarBloqueosVigentes(idDoctor));
    }
}
