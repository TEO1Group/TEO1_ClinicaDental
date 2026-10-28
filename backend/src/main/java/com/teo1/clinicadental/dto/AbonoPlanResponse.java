package com.teo1.clinicadental.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Abono registrado en un plan de tratamiento")
public record AbonoPlanResponse(
        @Schema(description = "Identificador del abono", example = "2b3c4d5e-6f7a-4b8c-9d0e-1f2a3b4c5d6e") UUID idAbono,
        @Schema(description = "Identificador del plan", example = "3b4c5d6e-7f8a-4b9c-8d0e-1f2a3b4c5d6e") UUID idPlan,
        @Schema(description = "Monto cobrado en quetzales", example = "150.50") BigDecimal monto,
        @Schema(description = "Fecha y hora de Guatemala generada por el servidor", example = "2026-10-21T10:15:00") LocalDateTime fecha,
        @Schema(description = "Usuario autenticado que registro el abono; null si fue eliminado",
                example = "4b5c6d7e-8f9a-4b0c-9d1e-2f3a4b5c6d7e", nullable = true) UUID idRegistradoPor) {
}
