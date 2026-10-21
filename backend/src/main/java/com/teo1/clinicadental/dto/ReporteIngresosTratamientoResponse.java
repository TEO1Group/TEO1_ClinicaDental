package com.teo1.clinicadental.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(description = "Tratamientos realizados en citas del periodo con lo cobrado; omite tratamientos sin registros")
public record ReporteIngresosTratamientoResponse(
        @Schema(description = "Primer dia incluido", example = "2026-10-01") LocalDate desde,
        @Schema(description = "Ultimo dia incluido", example = "2026-10-31") LocalDate hasta,
        @Schema(description = "Tratamientos realizados en total", example = "25") long cantidadTotal,
        @Schema(description = "Suma de lo cobrado en quetzales", example = "18450.00") BigDecimal ingresoTotal,
        @Schema(description = "Tratamientos ordenados de mayor a menor ingreso") List<IngresoTratamiento> tratamientos) {

    public record IngresoTratamiento(
            @Schema(description = "Identificador del tratamiento", example = "4e5f6a7b-8c9d-4e0f-9a1b-2c3d4e5f6a7b") UUID idTratamiento,
            @Schema(description = "Nombre del tratamiento", example = "Endodoncia") String nombre,
            @Schema(description = "Veces que se realizo en el periodo", example = "4") long cantidad,
            @Schema(description = "Suma del costo cobrado en quetzales", example = "7200.00") BigDecimal total) {
    }
}
