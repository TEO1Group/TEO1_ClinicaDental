package com.teo1.clinicadental.dto;

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
public class CrearPersonalRequest {

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
            message = "La clave debe contener al menos una mayuscula, una minuscula, un numero y un caracter especial"
    )
    private String password;

    @Pattern(regexp = "\\d{8}", message = "El telefono debe tener 8 digitos")
    private String telefono;

    @NotBlank
    private String idRol;

    private String especialidad;

    private String numeroColegiado;

    private String numeroClinica;

    private String turno;

    private String area;
}
