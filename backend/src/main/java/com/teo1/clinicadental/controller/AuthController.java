package com.teo1.clinicadental.controller;

import com.teo1.clinicadental.dto.RegistroRequest;
import com.teo1.clinicadental.dto.RegistroResponse;
import com.teo1.clinicadental.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.teo1.clinicadental.dto.LoginRequest;
import com.teo1.clinicadental.dto.LoginResponse;
import com.teo1.clinicadental.dto.UsuarioActualResponse;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import com.teo1.clinicadental.dto.ErrorResponse;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Registro, inicio de sesión y consulta de la sesión actual")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/registro")
    @Operation(summary = "Registrar una cuenta de cliente", description = "Pública. Registra una cuenta de cliente; devuelve 201.")
    @ApiResponse(responseCode = "201", description = "Cuenta registrada")
    @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "409", description = "Conflicto con una restricción de datos", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<RegistroResponse> registrar(
            @Valid @RequestBody RegistroRequest request
    ) {
        RegistroResponse response = authService.registrar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión", description = "Pública. Devuelve un JWT cuando las credenciales son válidas.")
    @ApiResponse(responseCode = "200", description = "Credenciales aceptadas; devuelve el token JWT")
    @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Credenciales no válidas", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "La cuenta está inactiva", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    @Operation(summary = "Obtener el usuario autenticado", description = "Requiere un JWT válido; devuelve el usuario autenticado y su perfil.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponse(responseCode = "200", description = "Usuario actual")
    @ApiResponse(responseCode = "401", description = "Falta autenticación válida", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<UsuarioActualResponse> usuarioActual(Authentication authentication) {
        return ResponseEntity.ok(authService.usuarioActual(UUID.fromString(authentication.getName())));
    }
}
