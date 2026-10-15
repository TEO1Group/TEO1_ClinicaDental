package com.teo1.clinicadental.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Actualización del paciente; se aplican solo los campos enviados")
public class ClienteUpdateRequest {

    @Size(max = 80)
    @Schema(description = "Nombre", example = "Ana")
    private String nombre;

    @Size(max = 80)
    @Schema(description = "Apellido", example = "Lopez")
    private String apellido;

    @Pattern(regexp = "\\d{8}", message = "El telefono debe tener 8 digitos")
    @Schema(description = "Teléfono de 8 dígitos", example = "55551234")
    private String telefono;

    @Size(max = 200)
    @Schema(description = "Dirección", example = "Zona 1, Ciudad de Guatemala")
    private String direccion;

    @Past
    @Schema(description = "Fecha de nacimiento, anterior a hoy", example = "1995-04-12")
    private LocalDate fechaNacimiento;
}
