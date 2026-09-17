package com.teo1.clinicadental.dto;

import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ClienteUpdateRequest {

    @Pattern(regexp = "\\d{8}", message = "El telefono debe tener 8 digitos")
    private String telefono;

    @Size(max = 200)
    private String direccion;

    @Past
    private LocalDate fechaNacimiento;
}
