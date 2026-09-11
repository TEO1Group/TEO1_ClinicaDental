package com.teo1.clinicadental.dto;

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
public class RegistroRequest {

    @NotBlank
    @Pattern(regexp = "\\d{13}")
    private String dpi;

    @NotBlank
    @Size(max = 80)
    private String nombre;

    @NotBlank
    @Size(max = 80)
    private String apellido;

    @NotBlank
    @Email
    @Size(max = 120)
    private String email;

    @NotBlank
    @Size(min = 8)
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9\\s]).+$",
            message = "La contraseña debe contener al menos una mayúscula, una minúscula, un número y un carácter especial"
    )
    private String password;

    // Campos nullable en el esquema (tabla cliente / usuario.telefono).
    @Pattern(regexp = "\\d{8}", message = "El teléfono debe tener 8 dígitos")
    private String telefono;

    @Size(max = 200)
    private String direccion;

    @Past
    private LocalDate fechaNacimiento;
}
