package com.teo1.clinicadental.controller;

import com.teo1.clinicadental.dto.CrearPersonalRequest;
import com.teo1.clinicadental.dto.EstadoUsuarioRequest;
import com.teo1.clinicadental.dto.RegistroResponse;
import com.teo1.clinicadental.dto.RolResponse;
import com.teo1.clinicadental.dto.UsuarioListadoResponse;
import com.teo1.clinicadental.dto.UsuarioUpdateRequest;
import com.teo1.clinicadental.service.AdminUsuarioService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@RestController
@RequiredArgsConstructor
@Tag(name = "Usuarios", description = "Administración de usuarios y catálogo de roles")
@SecurityRequirement(name = "bearerAuth")
public class UsuarioController {

    private final AdminUsuarioService adminUsuarioService;

    @GetMapping("/admin/usuarios")
    @Operation(summary = "Listar usuarios")
    @ApiResponse(responseCode = "200", description = "Lista de usuarios")
    @ApiResponse(responseCode = "401", description = "Falta autenticación válida")
    @ApiResponse(responseCode = "403", description = "Se requiere el rol ADMIN")
    public ResponseEntity<List<UsuarioListadoResponse>> listarUsuarios() {
        return ResponseEntity.ok(adminUsuarioService.listarUsuarios());
    }

    @PostMapping("/admin/usuarios")
    @Operation(summary = "Crear personal")
    @ApiResponse(responseCode = "201", description = "Personal creado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos")
    @ApiResponse(responseCode = "401", description = "Falta autenticación válida")
    @ApiResponse(responseCode = "403", description = "Se requiere el rol ADMIN")
    @ApiResponse(responseCode = "409", description = "Conflicto con una restricción de datos")
    public ResponseEntity<RegistroResponse> crearPersonal(@Valid @RequestBody CrearPersonalRequest request) {
        RegistroResponse response = adminUsuarioService.crearPersonal(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/admin/usuarios/{id}")
    @Operation(summary = "Actualizar un usuario")
    @ApiResponse(responseCode = "200", description = "Usuario actualizado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos o identificador no válido")
    @ApiResponse(responseCode = "401", description = "Falta autenticación válida")
    @ApiResponse(responseCode = "403", description = "Se requiere el rol ADMIN")
    @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    @ApiResponse(responseCode = "409", description = "El email ya está registrado")
    public ResponseEntity<UsuarioListadoResponse> actualizarUsuario(
            @PathVariable UUID id,
            @Valid @RequestBody UsuarioUpdateRequest request
    ) {
        return ResponseEntity.ok(adminUsuarioService.actualizarUsuario(id, request));
    }

    @PatchMapping("/admin/usuarios/{id}/estado")
    @Operation(summary = "Cambiar el estado de un usuario")
    @ApiResponse(responseCode = "200", description = "Estado actualizado")
    @ApiResponse(responseCode = "400", description = "Datos inválidos o identificador no válido")
    @ApiResponse(responseCode = "401", description = "Falta autenticación válida")
    @ApiResponse(responseCode = "403", description = "Se requiere el rol ADMIN")
    @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
    public ResponseEntity<UsuarioListadoResponse> cambiarEstado(
            @PathVariable UUID id,
            @Valid @RequestBody EstadoUsuarioRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(adminUsuarioService.cambiarEstado(id, request, authentication.getName()));
    }

    @GetMapping("/roles")
    @Operation(summary = "Listar roles")
    @ApiResponse(responseCode = "200", description = "Lista de roles")
    @ApiResponse(responseCode = "401", description = "Falta autenticación válida")
    @ApiResponse(responseCode = "403", description = "Se requiere el rol ADMIN")
    public ResponseEntity<List<RolResponse>> listarRoles() {
        return ResponseEntity.ok(adminUsuarioService.listarRoles());
    }
}
