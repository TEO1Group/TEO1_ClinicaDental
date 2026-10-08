package com.teo1.clinicadental.dto;

import com.teo1.clinicadental.model.EstadoCita;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "Cita agendada con los datos basicos del paciente y del doctor")
public class CitaResponse {

    @Schema(description = "Identificador de la cita", example = "3f1c2a9e-7b4d-4c1a-9e2f-5a6b7c8d9e01")
    private UUID idCita;

    @Schema(description = "Identificador del paciente", example = "8a2b3c4d-5e6f-4a1b-8c2d-3e4f5a6b7c8d")
    private UUID idCliente;

    @Schema(description = "Identificador del doctor", example = "1d2e3f4a-5b6c-4d7e-8f9a-0b1c2d3e4f5a")
    private UUID idDoctor;

    @Schema(description = "Fecha de la cita", example = "2026-10-15")
    private LocalDate fecha;

    @Schema(description = "Hora de inicio de la cita", type = "string", example = "09:30:00")
    private LocalTime hora;

    @Schema(description = "Estado de la cita", example = "AGENDADA")
    private EstadoCita estado;

    @Schema(description = "Notas o motivo de la cita", example = "Limpieza dental")
    private String notas;

    @Schema(description = "Nombre del paciente", example = "Ana")
    private String nombreCliente;

    @Schema(description = "Apellido del paciente", example = "Lopez")
    private String apellidoCliente;

    @Schema(description = "Nombre del doctor", example = "Carlos")
    private String nombreDoctor;

    @Schema(description = "Apellido del doctor", example = "Perez")
    private String apellidoDoctor;
}
