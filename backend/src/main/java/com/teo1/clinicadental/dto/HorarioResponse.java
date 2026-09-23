package com.teo1.clinicadental.dto;

import java.time.LocalTime;
import java.util.UUID;
import com.teo1.clinicadental.model.DiaSemana;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class HorarioResponse {

    private UUID idHorario;
    private UUID idDoctor;
    private DiaSemana diaSemana;
    private LocalTime horaInicio;
    private LocalTime horaFin;
}
