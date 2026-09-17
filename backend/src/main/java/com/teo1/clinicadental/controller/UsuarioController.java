package com.teo1.clinicadental.controller;

import com.teo1.clinicadental.dto.CrearPersonalRequest;
import com.teo1.clinicadental.dto.RegistroResponse;
import com.teo1.clinicadental.dto.RolResponse;
import com.teo1.clinicadental.dto.UsuarioListadoResponse;
import com.teo1.clinicadental.service.AdminUsuarioService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
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

    @GetMapping("/roles")
    public ResponseEntity<List<RolResponse>> listarRoles() {
        return ResponseEntity.ok(adminUsuarioService.listarRoles());
    }
}
