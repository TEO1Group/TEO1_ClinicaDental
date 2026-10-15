package com.teo1.clinicadental.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Credenciales de una cuenta activa")
public class LoginRequest {

    @NotBlank
    @Email
    @Schema(description = "Correo electrónico", example = "ana@example.com")
    private String email;

    @NotBlank
    @Schema(description = "Contraseña de la cuenta existente; ejemplo ficticio", example = "Ejemplo!2026", format = "password", accessMode = Schema.AccessMode.WRITE_ONLY)
    private String password;
}
