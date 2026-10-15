package com.teo1.clinicadental.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Nueva fecha y hora de la cita y motivo del cambio")
public class ReprogramarCitaRequest {

    @NotNull(message = "La fecha es obligatoria")
    @Schema(description = "Nueva fecha en formato yyyy-MM-dd", example = "2026-10-22", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate fecha;

    @NotNull(message = "La hora es obligatoria")
    @Schema(description = "Nueva hora de inicio en formato HH:mm", type = "string", example = "10:00", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalTime hora;

    @NotBlank(message = "El motivo es obligatorio")
    @Size(max = 200, message = "El motivo no puede superar 200 caracteres")
    @Schema(description = "Motivo de la reprogramacion", example = "Cambio de turno en el trabajo del paciente", maxLength = 200, requiredMode = Schema.RequiredMode.REQUIRED)
    private String motivo;
}
