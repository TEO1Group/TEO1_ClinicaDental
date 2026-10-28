package com.teo1.clinicadental.controller;

import com.teo1.clinicadental.dto.ErrorResponse;
import com.teo1.clinicadental.dto.PlanTratamientoResponse;
import com.teo1.clinicadental.service.PlanTratamientoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Planes de tratamiento")
@SecurityRequirement(name = "bearerAuth")
public class MisPlanesController {

    private final PlanTratamientoService planTratamientoService;

    @GetMapping("/mis-planes")
    @Operation(summary = "Consultar mis planes", description = "Solo CLIENTE, en modo lectura. El paciente se identifica exclusivamente por el usuario del JWT; no se recibe un idCliente. Devuelve unicamente sus planes, del mas reciente al mas antiguo, con avance, costo total, abonos acumulados y saldo, sin lista de citas. Lista vacia si no tiene planes.")
    @ApiResponse(responseCode = "200", description = "Planes del paciente autenticado")
    @ApiResponse(responseCode = "401", description = "Falta autenticacion valida", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Solo CLIENTE", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "404", description = "El usuario no tiene un perfil de paciente", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<List<PlanTratamientoResponse>> listar(Authentication authentication) {
        return ResponseEntity.ok(planTratamientoService.listarMisPlanes(authentication));
    }
}
