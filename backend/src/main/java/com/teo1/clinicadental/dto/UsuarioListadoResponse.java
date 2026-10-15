package com.teo1.clinicadental.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "Usuario del personal o cliente, incluida su cuenta activa o inactiva")
public class UsuarioListadoResponse {

    @Schema(description = "Identificador del usuario", example = "1d2e3f4a-5b6c-4d7e-8f9a-0b1c2d3e4f5a")
    private UUID idUsuario;
    @Schema(description = "Nombre", example = "Ana")
    private String nombre;
    @Schema(description = "Apellido", example = "Lopez")
    private String apellido;
    @Schema(description = "Correo electrónico", example = "ana@example.com")
    private String email;
    @Schema(description = "Teléfono de 8 dígitos", example = "55551234")
    private String telefono;
    @Schema(description = "Rol de la cuenta", allowableValues = {"ADMIN", "SECRETARIA", "DOCTOR", "CLIENTE"}, example = "CLIENTE")
    private String rol;
    @Schema(description = "Estado de la cuenta", allowableValues = {"ACTIVO", "INACTIVO"}, example = "ACTIVO")
    private String estado;
    @Schema(description = "Fecha de creación de la cuenta", example = "2026-10-15T10:00:00")
    private LocalDateTime fechaCreacion;
}
