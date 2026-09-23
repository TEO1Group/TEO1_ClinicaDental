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
public class DoctorUpdateRequest {

    @Size(max = 80)
    private String nombre;

    @Size(max = 80)
    private String apellido;

    @Pattern(regexp = "\\d{8}", message = "El telefono debe tener 8 digitos")
    private String telefono;

    @NotBlank(message = "La especialidad es obligatoria")
    @Size(max = 100)
    private String especialidad;

    @Size(max = 30)
    private String numeroColegiado;

    @Size(max = 20)
    private String numeroClinica;
}
