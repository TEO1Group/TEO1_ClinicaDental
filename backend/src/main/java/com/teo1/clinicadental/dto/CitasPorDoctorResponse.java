package com.teo1.clinicadental.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "Cantidad de citas de un doctor por estado en el periodo")
public class CitasPorDoctorResponse {

    @Schema(description = "Identificador del doctor", example = "1d2e3f4a-5b6c-4d7e-8f9a-0b1c2d3e4f5a")
    private UUID idDoctor;

    @Schema(description = "Nombre del doctor", example = "Carlos")
    private String nombreDoctor;

    @Schema(description = "Apellido del doctor", example = "Perez")
    private String apellidoDoctor;

    @Schema(description = "Especialidad del doctor", example = "Ortodoncia")
    private String especialidad;

    @Schema(description = "Citas en estado AGENDADA", example = "4")
    private long agendadas;

    @Schema(description = "Citas en estado ATENDIDA", example = "10")
    private long atendidas;

    @Schema(description = "Citas en estado CANCELADA", example = "2")
    private long canceladas;

    @Schema(description = "Citas en estado NO_ASISTIO", example = "1")
    private long noAsistio;

    @Schema(description = "Total de citas del doctor en el periodo", example = "17")
    private long total;
}
