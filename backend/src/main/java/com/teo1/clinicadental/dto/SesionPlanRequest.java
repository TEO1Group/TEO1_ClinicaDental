package com.teo1.clinicadental.dto;

import io.swagger.v3.oas.annotations.media.Schema;
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
@Schema(description = "Fecha y hora de una sesion del plan; el paciente y el doctor son los del plan")
public class SesionPlanRequest {

    @NotNull(message = "La fecha es obligatoria")
    @Schema(description = "Fecha de la sesion en formato yyyy-MM-dd", example = "2026-10-28", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate fecha;

    @NotNull(message = "La hora es obligatoria")
    @Schema(description = "Hora de inicio en formato HH:mm", type = "string", example = "09:30", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalTime hora;

    @Size(max = 500)
    @Schema(description = "Notas de la cita; si se omiten se usa 'Sesion n de N: tratamiento'", example = "Traer radiografia", maxLength = 500)
    private String notas;
}
