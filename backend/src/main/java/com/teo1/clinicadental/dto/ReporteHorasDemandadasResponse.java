package com.teo1.clinicadental.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "Citas de todos los estados y doctores agrupadas por hora de inicio; omite horas sin citas")
public record ReporteHorasDemandadasResponse(
        @Schema(description = "Primer dia incluido", example = "2026-10-01") LocalDate desde,
        @Schema(description = "Ultimo dia incluido", example = "2026-10-31") LocalDate hasta,
        @Schema(description = "Horas con citas, ordenadas de 0 a 23") List<HoraDemandada> horas) {

    public record HoraDemandada(
            @Schema(description = "Hora de inicio sin los minutos", example = "9", minimum = "0", maximum = "23") int hora,
            @Schema(description = "Cantidad de citas en esa hora", example = "12") long cantidad) {
    }
}
