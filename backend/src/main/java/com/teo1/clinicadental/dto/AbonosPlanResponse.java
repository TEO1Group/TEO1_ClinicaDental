package com.teo1.clinicadental.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Schema(description = "Abonos y saldo del plan, calculados en la misma transaccion de lectura")
public record AbonosPlanResponse(
        @Schema(description = "Identificador del plan", example = "3b4c5d6e-7f8a-4b9c-8d0e-1f2a3b4c5d6e") UUID idPlan,
        @Schema(description = "Costo total pactado en quetzales", example = "1000.00") BigDecimal costoTotal,
        @Schema(description = "Suma de abonos registrados", example = "150.50") BigDecimal totalAbonado,
        @Schema(description = "Costo total menos el total abonado", example = "849.50") BigDecimal saldo,
        @Schema(description = "Abonos ordenados por fecha y luego identificador; lista vacia si no hay") List<AbonoPlanResponse> abonos) {
}
