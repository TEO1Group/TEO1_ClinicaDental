package com.teo1.clinicadental.controller;

import com.teo1.clinicadental.dto.ErrorResponse;
import com.teo1.clinicadental.dto.TratamientoRequest;
import com.teo1.clinicadental.dto.TratamientoResponse;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
            description = "Requiere rol ADMIN, DOCTOR o SECRETARIA. Devuelve los activos ordenados por nombre. Solo ADMIN puede usar incluirInactivos=true. Incluye el "
                    + "costo sugerido que se usa por defecto al registrar un tratamiento en una cita."
    )
    @ApiResponse(responseCode = "200", description = "Catalogo de tratamientos")
    @ApiResponse(responseCode = "401", description = "Falta autenticacion valida", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Se requiere ADMIN, DOCTOR o SECRETARIA; solo ADMIN puede incluir inactivos", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "400", description = "incluirInactivos no es booleano", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<List<TratamientoResponse>> listarCatalogo(
            @RequestParam(defaultValue = "false") boolean incluirInactivos, Authentication authentication) {
        return ResponseEntity.ok(tratamientoService.listarCatalogo(incluirInactivos, authentication));
    }

    @PostMapping
    @Operation(summary = "Crear tratamiento del catalogo", description = "Solo ADMIN. Nombre unico (sensible a mayusculas), costo no negativo y duracion positiva. Activo por defecto; no altera registros anteriores.")
    @ApiResponse(responseCode = "201", description = "Tratamiento creado")
    @ApiResponse(responseCode = "400", description = "Nombre ausente/vacio o mayor de 100, costo ausente/negativo/fuera de precision, duracion no positiva, activo nulo o formato invalido", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Falta autenticacion valida", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Solo ADMIN", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "El nombre ya existe", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<TratamientoResponse> crear(@Valid @RequestBody TratamientoRequest request, Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tratamientoService.crearCatalogo(request, authentication));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar o desactivar tratamiento", description = "Solo ADMIN. Reemplaza nombre, descripcion, duracion, costo sugerido y activo (true si se omite). No elimina el tratamiento ni modifica costos ya cobrados. Un inactivo no admite nuevos registros; su historial permanece disponible.")
    @ApiResponse(responseCode = "200", description = "Tratamiento actualizado")
    @ApiResponse(responseCode = "404", description = "Tratamiento no encontrado", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "400", description = "Nombre ausente/vacio o mayor de 100, costo ausente/negativo/fuera de precision, duracion no positiva, activo nulo o formato invalido", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Falta autenticacion valida", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Solo ADMIN", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "El nombre ya existe", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<TratamientoResponse> actualizar(@PathVariable UUID id, @Valid @RequestBody TratamientoRequest request, Authentication authentication) {
        return ResponseEntity.ok(tratamientoService.actualizarCatalogo(id, request, authentication));
    }
}
