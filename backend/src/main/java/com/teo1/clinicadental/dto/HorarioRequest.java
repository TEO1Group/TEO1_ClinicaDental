package com.teo1.clinicadental.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;
import com.teo1.clinicadental.model.DiaSemana;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class HorarioRequest {

    @NotNull
    private DiaSemana diaSemana;

    @NotNull
    private LocalTime horaInicio;

    @NotNull
    private LocalTime horaFin;
}
