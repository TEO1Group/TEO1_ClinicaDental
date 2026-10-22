package com.teo1.clinicadental.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "Estado de inasistencias de un paciente")
public class InasistenciasPacienteResponse {

    @Schema(description = "Identificador del paciente", example = "3f2a1b4c-5d6e-4f70-8a9b-0c1d2e3f4a5b")
    private UUID idCliente;

    @Schema(description = "Citas NO_ASISTIO desde el ultimo levantamiento (o desde siempre si nunca se levanto)", example = "2")
    private long inasistencias;

    @Schema(description = "Cantidad de inasistencias con la que el paciente ya no puede agendar", example = "3")
    private int limite;

    @Schema(description = "true si alcanzo el limite de inasistencias; no representa otras restricciones del paciente", example = "false")
    private boolean bloqueado;

    @Schema(description = "Ultimo levantamiento de la restriccion; null si nunca se le levanto", nullable = true)
    private LevantamientoInasistenciaResponse ultimoLevantamiento;
}
