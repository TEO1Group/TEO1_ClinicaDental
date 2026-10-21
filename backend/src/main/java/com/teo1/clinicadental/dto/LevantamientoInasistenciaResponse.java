package com.teo1.clinicadental.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "Registro de un levantamiento de la restriccion por inasistencias")
public class LevantamientoInasistenciaResponse {

    @Schema(description = "Identificador del levantamiento", example = "6c7d8e9f-0a1b-4c2d-8e3f-4a5b6c7d8e9f")
    private UUID idLevantamiento;

    @Schema(description = "Motivo del levantamiento", example = "Presento constancia medica de sus ausencias")
    private String motivo;

    @Schema(description = "Inasistencias que tenia el paciente al levantarle la restriccion", example = "3")
    private int inasistencias;

    @Schema(description = "Usuario que levanto la restriccion; null si ese usuario ya no existe", example = "7c8d9e0f-1a2b-4c3d-8e4f-5a6b7c8d9e0f", nullable = true)
    private UUID idLevantadoPor;

    @Schema(description = "Fecha y hora del levantamiento en hora de Guatemala; las inasistencias se cuentan desde aqui", type = "string", example = "2026-10-21T10:15:00")
    private LocalDateTime fecha;
}
