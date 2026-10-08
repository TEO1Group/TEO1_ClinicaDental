package com.teo1.clinicadental.dto;

import com.teo1.clinicadental.model.EstadoCita;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Nuevo estado de la cita")
public class EstadoCitaRequest {

    @NotNull(message = "El estado es obligatorio")
    @Schema(description = "Estado final: ATENDIDA, CANCELADA o NO_ASISTIO", example = "CANCELADA", requiredMode = Schema.RequiredMode.REQUIRED)
    private EstadoCita estado;
}
