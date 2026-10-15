package com.teo1.clinicadental.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Registro de un paciente activo con rol CLIENTE; email y DPI deben ser únicos")
public class RegistroRequest {

    @NotBlank
    @Pattern(regexp = "\\d{13}", message = "El DPI debe tener 13 dígitos")
    @Schema(description = "DPI de 13 dígitos", example = "1234567890123")
    private String dpi;

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
            message = "La contraseña debe contener al menos una mayúscula, una minúscula, un número y un carácter especial"
    )
    @Schema(description = "Contraseña; ejemplo ficticio. Al registrar requiere 8 caracteres, mayúscula, minúscula, número y símbolo", example = "Ejemplo!2026", format = "password", accessMode = Schema.AccessMode.WRITE_ONLY)
    private String password;

    // Campos nullable en el esquema (tabla cliente / usuario.telefono).
    @Pattern(regexp = "\\d{8}", message = "El teléfono debe tener 8 dígitos")
    @Schema(description = "Teléfono de 8 dígitos", example = "55551234")
    private String telefono;

    @Size(max = 200)
    @Schema(description = "Dirección", example = "Zona 1, Ciudad de Guatemala")
    private String direccion;

    @Past
    @Schema(description = "Fecha de nacimiento, anterior a hoy", example = "1995-04-12")
    private LocalDate fechaNacimiento;
}
