package com.teo1.clinicadental.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(description = "Citas NO_ASISTIO y CANCELADA del periodo, por doctor y por paciente; solo aparecen quienes tienen alguna")
public record ReporteInasistenciasResponse(
        @Schema(description = "Primer dia incluido", example = "2026-10-01") LocalDate desde,
        @Schema(description = "Ultimo dia incluido", example = "2026-10-31") LocalDate hasta,
        @Schema(description = "Total de citas NO_ASISTIO del periodo", example = "7") long totalNoAsistio,
        @Schema(description = "Total de citas CANCELADA del periodo", example = "12") long totalCanceladas,
        @Schema(description = "Suma de inasistencias y cancelaciones", example = "19") long total,
        @Schema(description = "Doctores ordenados de mayor a menor total") List<PorDoctor> doctores,
        @Schema(description = "Pacientes ordenados de mayor a menor total") List<PorPaciente> pacientes) {

    public record PorDoctor(
            @Schema(description = "Identificador del doctor", example = "1d2e3f4a-5b6c-4d7e-8f9a-0b1c2d3e4f5a") UUID idDoctor,
            @Schema(description = "Nombre del doctor", example = "Carlos") String nombreDoctor,
            @Schema(description = "Apellido del doctor", example = "Perez") String apellidoDoctor,
            @Schema(description = "Citas NO_ASISTIO", example = "3") long noAsistio,
            @Schema(description = "Citas CANCELADA", example = "5") long canceladas,
            @Schema(description = "Inasistencias mas cancelaciones", example = "8") long total) {
    }

    public record PorPaciente(
            @Schema(description = "Identificador del paciente", example = "8a2b3c4d-5e6f-4a1b-8c2d-3e4f5a6b7c8d") UUID idCliente,
            @Schema(description = "Nombre del paciente", example = "Ana") String nombreCliente,
            @Schema(description = "Apellido del paciente", example = "Lopez") String apellidoCliente,
            @Schema(description = "Citas NO_ASISTIO", example = "2") long noAsistio,
            @Schema(description = "Citas CANCELADA", example = "1") long canceladas,
            @Schema(description = "Inasistencias mas cancelaciones", example = "3") long total) {
    }
}
