package com.teo1.clinicadental.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;
import com.teo1.clinicadental.model.DiaSemana;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Horario de atención; fin posterior a inicio y sin solapamientos el mismo día")
public class HorarioRequest {

    @NotNull
    @Schema(description = "Día de atención", example = "LUNES")
    private DiaSemana diaSemana;

    @NotNull
    @Schema(description = "Inicio del horario, hora de Guatemala", type = "string", example = "08:00:00")
    private LocalTime horaInicio;

    @NotNull
    @Schema(description = "Fin del horario, posterior al inicio", type = "string", example = "12:00:00")
    private LocalTime horaFin;
}
