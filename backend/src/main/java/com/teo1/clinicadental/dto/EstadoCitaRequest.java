package com.teo1.clinicadental.dto;

import com.teo1.clinicadental.model.EstadoCita;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Nuevo estado de la cita")
public class EstadoCitaRequest {

    @NotNull(message = "El estado es obligatorio")
    @Schema(
            description = "Estado final: ATENDIDA, CANCELADA o NO_ASISTIO",
            allowableValues = {"ATENDIDA", "CANCELADA", "NO_ASISTIO"},
            example = "CANCELADA",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private EstadoCita estado;

    @Size(max = 200, message = "El motivo no puede superar 200 caracteres")
    @Schema(
            description = "Motivo del cambio. Obligatorio para CANCELADA y NO_ASISTIO, opcional para ATENDIDA. "
                    + "Se recortan los espacios; si queda vacio no se guarda",
            example = "El paciente tuvo un imprevisto de trabajo",
            maxLength = 200
    )
    private String motivo;
}
