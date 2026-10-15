package com.teo1.clinicadental.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Entrada de texto no vacío para el historial clínico")
public class HistorialClinicoRequest {

    @NotBlank
    @Schema(description = "Descripción", example = "Control clínico de seguimiento")
    private String descripcion;
}
