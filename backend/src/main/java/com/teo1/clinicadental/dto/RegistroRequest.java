package com.teo1.clinicadental.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class RegistroRequest {

    @NotBlank
    @Pattern(regexp = "\\d{13}")
    private String dpi;

    @NotBlank
    @Size(max = 100)
    private String nombre;

    @NotBlank
    @Pattern(regexp = "\\d{1,15}")
    private String celular;

    @NotBlank
    @Size(min = 8)
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s]).+$",
            message = "La contraseña debe contener al menos una mayúscula, una minúscula, un número y un carácter especial"
    )
    private String password;
}
