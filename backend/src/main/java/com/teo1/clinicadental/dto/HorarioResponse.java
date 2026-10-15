package com.teo1.clinicadental.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalTime;
import java.util.UUID;
import com.teo1.clinicadental.model.DiaSemana;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "Horario de atención del doctor")
public class HorarioResponse {

    @Schema(description = "Identificador del horario", example = "1d2e3f4a-5b6c-4d7e-8f9a-0b1c2d3e4f5a")
    private UUID idHorario;
    @Schema(description = "Identificador del doctor", example = "1d2e3f4a-5b6c-4d7e-8f9a-0b1c2d3e4f5a")
    private UUID idDoctor;
    @Schema(description = "Día de atención", example = "LUNES")
    private DiaSemana diaSemana;
    @Schema(description = "Inicio del horario, hora de Guatemala", type = "string", example = "08:00:00")
    private LocalTime horaInicio;
    @Schema(description = "Fin del horario, posterior al inicio", type = "string", example = "12:00:00")
    private LocalTime horaFin;
}
