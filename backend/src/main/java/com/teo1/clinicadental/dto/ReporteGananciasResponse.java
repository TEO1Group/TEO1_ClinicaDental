package com.teo1.clinicadental.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(description = "Ingresos del periodo: lo cobrado en tratamientos por doctor y, aparte, lo abonado a planes")
public record ReporteGananciasResponse(
        @Schema(description = "Primer dia incluido", example = "2026-10-01") LocalDate desde,
        @Schema(description = "Ultimo dia incluido", example = "2026-10-31") LocalDate hasta,
        @Schema(description = "Suma del costo cobrado en tratamientos de citas ATENDIDA del periodo, en quetzales", example = "18450.00") BigDecimal totalTratamientos,
        @Schema(description = "Suma de los abonos a planes registrados en el periodo, en quetzales. Se informa aparte y no esta incluida en totalTratamientos", example = "3500.00") BigDecimal totalAbonos,
        @Schema(description = "Doctores activos y los que tuvieron ingresos en el periodo, ordenados por apellido y nombre") List<GananciaDoctor> doctores) {

    public record GananciaDoctor(
            @Schema(description = "Identificador del doctor", example = "1d2e3f4a-5b6c-4d7e-8f9a-0b1c2d3e4f5a") UUID idDoctor,
            @Schema(description = "Nombre del doctor", example = "Carlos") String nombreDoctor,
            @Schema(description = "Apellido del doctor", example = "Perez") String apellidoDoctor,
            @Schema(description = "Tratamientos realizados en el periodo", example = "12") long cantidadTratamientos,
            @Schema(description = "Suma del costo cobrado en quetzales", example = "9600.00") BigDecimal total) {
    }
}
