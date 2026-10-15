package com.teo1.clinicadental.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "Periodo en el que el doctor no atiende")
public class BloqueoDoctorResponse {

    @Schema(description = "Identificador del bloqueo", example = "5b6c7d8e-9f0a-4b1c-8d2e-3f4a5b6c7d8e")
    private UUID idBloqueo;

    @Schema(description = "Identificador del doctor", example = "1d2e3f4a-5b6c-4d7e-8f9a-0b1c2d3e4f5a")
    private UUID idDoctor;

    @Schema(description = "Inicio del bloqueo en hora de Guatemala", type = "string", example = "2026-10-20T08:00:00")
    private LocalDateTime inicio;

    @Schema(description = "Fin del bloqueo en hora de Guatemala", type = "string", example = "2026-10-20T12:00:00")
    private LocalDateTime fin;

    @Schema(description = "Motivo del bloqueo", example = "Congreso de ortodoncia")
    private String motivo;

    @Schema(description = "Usuario que creo el bloqueo; null si ese usuario ya no existe", example = "7c8d9e0f-1a2b-4c3d-8e4f-5a6b7c8d9e0f", nullable = true)
    private UUID idCreadoPor;

    @Schema(description = "Fecha y hora en que se creo el bloqueo", type = "string", example = "2026-10-15T09:12:30")
    private LocalDateTime fechaCreacion;
}
