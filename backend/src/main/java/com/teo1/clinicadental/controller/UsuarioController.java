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

@RestController
@RequiredArgsConstructor
public class UsuarioController {

    private final AdminUsuarioService adminUsuarioService;

    @GetMapping("/admin/usuarios")
    public ResponseEntity<List<UsuarioListadoResponse>> listarUsuarios() {
        return ResponseEntity.ok(adminUsuarioService.listarUsuarios());
    }

    @PostMapping("/admin/usuarios")
    public ResponseEntity<RegistroResponse> crearPersonal(@Valid @RequestBody CrearPersonalRequest request) {
        RegistroResponse response = adminUsuarioService.crearPersonal(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/admin/usuarios/{id}")
    public ResponseEntity<UsuarioListadoResponse> actualizarUsuario(
            @PathVariable UUID id,
            @Valid @RequestBody UsuarioUpdateRequest request
    ) {
        return ResponseEntity.ok(adminUsuarioService.actualizarUsuario(id, request));
    }

    @PatchMapping("/admin/usuarios/{id}/estado")
    public ResponseEntity<UsuarioListadoResponse> cambiarEstado(
            @PathVariable UUID id,
            @Valid @RequestBody EstadoUsuarioRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(adminUsuarioService.cambiarEstado(id, request, authentication.getName()));
    }

    @GetMapping("/roles")
    public ResponseEntity<List<RolResponse>> listarRoles() {
        return ResponseEntity.ok(adminUsuarioService.listarRoles());
    }
}
