package com.teo1.clinicadental.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "Tratamiento registrado en una cita")
public class CitaTratamientoResponse {

    @Schema(description = "Identificador del registro", example = "9e8d7c6b-5a4f-4e3d-8c2b-1a0f9e8d7c6b")
    private UUID idCitaTratamiento;

    @Schema(description = "Identificador de la cita", example = "3f1c2a9e-7b4d-4c1a-9e2f-5a6b7c8d9e01")
    private UUID idCita;

    @Schema(description = "Fecha de la cita", example = "2026-10-15")
    private LocalDate fechaCita;

    @Schema(description = "Hora de inicio de la cita", type = "string", example = "09:30:00")
    private LocalTime horaCita;

    @Schema(description = "Nombre del doctor de la cita", example = "Carlos")
    private String nombreDoctor;

    @Schema(description = "Apellido del doctor de la cita", example = "Perez")
    private String apellidoDoctor;

    @Schema(description = "Identificador del tratamiento", example = "2a3b4c5d-6e7f-4a8b-9c0d-1e2f3a4b5c6d")
    private UUID idTratamiento;

    @Schema(description = "Nombre del tratamiento", example = "Extraccion simple")
    private String nombreTratamiento;

    @Schema(description = "Costo cobrado en quetzales", example = "350.00")
    private BigDecimal costo;

    @Schema(description = "Observaciones del doctor", example = "Pieza 36, sin complicaciones", nullable = true)
    private String observaciones;

    @Schema(description = "Fecha y hora en que se registro", type = "string", example = "2026-10-15T10:05:00")
    private LocalDateTime fechaRegistro;
}
