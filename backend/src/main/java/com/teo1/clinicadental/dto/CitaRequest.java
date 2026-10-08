package com.teo1.clinicadental.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Datos para agendar una cita")
public class CitaRequest {

    @NotNull
    @Schema(description = "Identificador del doctor", example = "1d2e3f4a-5b6c-4d7e-8f9a-0b1c2d3e4f5a", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID idDoctor;

    @Schema(description = "Identificador del paciente. Obligatorio para ADMIN y SECRETARIA; se ignora si agenda un CLIENTE", example = "8a2b3c4d-5e6f-4a1b-8c2d-3e4f5a6b7c8d")
    private UUID idCliente;

    @NotNull
    @Schema(description = "Fecha de la cita en formato yyyy-MM-dd", example = "2026-10-15", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDate fecha;

    @NotNull
    @Schema(description = "Hora de inicio en formato HH:mm", type = "string", example = "09:30", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalTime hora;

    @Size(max = 500)
    @Schema(description = "Notas o motivo de la cita", example = "Limpieza dental", maxLength = 500)
    private String notas;
}
