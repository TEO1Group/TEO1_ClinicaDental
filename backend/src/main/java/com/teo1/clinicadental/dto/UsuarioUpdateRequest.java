package com.teo1.clinicadental.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Actualización de campos enviados; nombre, apellido y email vacíos se ignoran")
public class UsuarioUpdateRequest {

    @Size(max = 80)
    @Schema(description = "Nombre", example = "Ana")
    private String nombre;

    @Size(max = 80)
    @Schema(description = "Apellido", example = "Lopez")
    private String apellido;

    @Email
    @Size(max = 120)
    @Schema(description = "Correo electrónico", example = "ana@example.com")
    private String email;

    @Pattern(regexp = "\\d{8}", message = "El telefono debe tener 8 digitos")
    @Schema(description = "Teléfono de 8 dígitos", example = "55551234")
    private String telefono;
}
