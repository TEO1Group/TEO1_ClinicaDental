package com.teo1.clinicadental.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "Periodo en el que el doctor no atiende y que aun no termina")
public class BloqueoVigenteResponse {

    @Schema(description = "Inicio del bloqueo en hora de Guatemala", type = "string", example = "2026-10-20T08:00:00")
    private LocalDateTime inicio;

    @Schema(description = "Fin del bloqueo en hora de Guatemala", type = "string", example = "2026-10-20T12:00:00")
    private LocalDateTime fin;

    @Schema(description = "Motivo del bloqueo", example = "Congreso de ortodoncia")
    private String motivo;
}
