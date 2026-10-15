package com.teo1.clinicadental.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "Reporte de citas por doctor y estado en un periodo")
public class ReporteCitasPorDoctorResponse {

    @Schema(description = "Primer dia del periodo, incluido", example = "2026-10-01")
    private LocalDate desde;

    @Schema(description = "Ultimo dia del periodo, incluido", example = "2026-10-31")
    private LocalDate hasta;

    @Schema(description = "Doctores activos ordenados por apellido y nombre, con ceros si no tienen citas en el periodo")
    private List<CitasPorDoctorResponse> doctores;
}
