package com.teo1.clinicadental.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "Entrada del historial clínico; fecha asignada por el servidor")
public class HistorialClinicoResponse {

    @Schema(description = "Identificador de la entrada del historial", example = "1d2e3f4a-5b6c-4d7e-8f9a-0b1c2d3e4f5a")
    private UUID idHistorial;
    @Schema(description = "Identificador del paciente", example = "1d2e3f4a-5b6c-4d7e-8f9a-0b1c2d3e4f5a")
    private UUID idCliente;
    @Schema(description = "Fecha de registro", example = "2026-10-15T10:00:00")
    private LocalDateTime fecha;
    @Schema(description = "Descripción", example = "Control clínico de seguimiento")
    private String descripcion;
}
