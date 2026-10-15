package com.teo1.clinicadental.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Actualización del doctor; especialidad obligatoria, número de colegiado y clínica se reemplazan incluso si se omiten")
public class DoctorUpdateRequest {

    @Size(max = 80)
    @Schema(description = "Nombre", example = "Ana")
    private String nombre;

    @Size(max = 80)
    @Schema(description = "Apellido", example = "Lopez")
    private String apellido;

    @Pattern(regexp = "\\d{8}", message = "El telefono debe tener 8 digitos")
    @Schema(description = "Teléfono de 8 dígitos", example = "55551234")
    private String telefono;

    @NotBlank(message = "La especialidad es obligatoria")
    @Size(max = 100)
    @Schema(description = "Especialidad del doctor", example = "Ortodoncia")
    private String especialidad;

    @Size(max = 30)
    @Schema(description = "Número de colegiado", example = "COL-1234")
    private String numeroColegiado;

    @Size(max = 20)
    @Schema(description = "Número de clínica", example = "101")
    private String numeroClinica;
}
