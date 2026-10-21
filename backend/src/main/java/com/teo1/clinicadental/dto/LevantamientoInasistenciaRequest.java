package com.teo1.clinicadental.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Levantamiento de la restriccion por inasistencias")
public class LevantamientoInasistenciaRequest {

    @NotBlank(message = "El motivo es obligatorio")
    @Size(max = 200, message = "El motivo no puede superar 200 caracteres")
    @Schema(description = "Por que se le levanta la restriccion al paciente", example = "Presento constancia medica de sus ausencias", maxLength = 200, requiredMode = Schema.RequiredMode.REQUIRED)
    private String motivo;
}
