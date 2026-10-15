package com.teo1.clinicadental.controller;

import com.teo1.clinicadental.dto.ErrorResponse;
import com.teo1.clinicadental.dto.TratamientoResponse;
import com.teo1.clinicadental.service.TratamientoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tratamientos")
@RequiredArgsConstructor
@Tag(name = "Tratamientos", description = "Catalogo de tratamientos y tratamientos realizados en las citas")
@SecurityRequirement(name = "bearerAuth")
public class TratamientoController {

    private final TratamientoService tratamientoService;

    @GetMapping
    @Operation(
            summary = "Listar el catalogo de tratamientos",
            description = "Requiere rol ADMIN, DOCTOR o SECRETARIA. Devuelve todo el catalogo ordenado por nombre, con el "
                    + "costo sugerido que se usa por defecto al registrar un tratamiento en una cita."
    )
    @ApiResponse(responseCode = "200", description = "Catalogo de tratamientos")
    @ApiResponse(responseCode = "401", description = "Falta autenticacion valida", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Se requiere el rol ADMIN, DOCTOR o SECRETARIA", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<List<TratamientoResponse>> listarCatalogo() {
        return ResponseEntity.ok(tratamientoService.listarCatalogo());
    }
}
