package com.teo1.clinicadental.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Creación de una cuenta activa de personal; especialidad obligatoria para DOCTOR")
public class CrearPersonalRequest {

    @NotBlank
    @Size(max = 80)
    @Schema(description = "Nombre", example = "Ana")
    private String nombre;

    @NotBlank
    @Size(max = 80)
    @Schema(description = "Apellido", example = "Lopez")
    private String apellido;

    @NotBlank
    @Email
    @Size(max = 120)
    @Schema(description = "Correo electrónico", example = "ana@example.com")
    private String email;

    @NotBlank
    @Size(min = 8)
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s]).+$",
            message = "La clave debe contener al menos una mayuscula, una minuscula, un numero y un caracter especial"
    )
    @Schema(description = "Contraseña; ejemplo ficticio. Al registrar requiere 8 caracteres, mayúscula, minúscula, número y símbolo", example = "Ejemplo!2026", format = "password", accessMode = Schema.AccessMode.WRITE_ONLY)
    private String password;

    @Pattern(regexp = "\\d{8}", message = "El telefono debe tener 8 digitos")
    @Schema(description = "Teléfono de 8 dígitos", example = "55551234")
    private String telefono;

    @NotBlank
    @Schema(description = "Nombre del rol, no un UUID; CLIENTE se registra en /auth/registro", allowableValues = {"DOCTOR", "SECRETARIA", "ADMIN"}, example = "DOCTOR")
    private String idRol;

    @Schema(description = "Especialidad del doctor", example = "Ortodoncia")
    private String especialidad;

    @Schema(description = "Número de colegiado", example = "COL-1234")
    private String numeroColegiado;

    @Schema(description = "Número de clínica", example = "101")
    private String numeroClinica;

    @Schema(description = "Turno opcional, solo se aplica al crear SECRETARIA", allowableValues = {"MANANA", "TARDE", "NOCHE"}, example = "MANANA")
    private String turno;

    @Schema(description = "Área de SECRETARIA; por defecto RECEPCION si está vacía", example = "RECEPCION")
    private String area;
}
