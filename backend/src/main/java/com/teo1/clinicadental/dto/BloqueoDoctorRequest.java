package com.teo1.clinicadental.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Periodo en el que el doctor no atiende")
public class BloqueoDoctorRequest {

    @NotNull(message = "El inicio es obligatorio")
    @Schema(description = "Inicio del bloqueo en hora de Guatemala, formato yyyy-MM-ddTHH:mm:ss", type = "string", example = "2026-10-20T08:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime inicio;

    @NotNull(message = "El fin es obligatorio")
    @Schema(description = "Fin del bloqueo en hora de Guatemala, debe ser posterior al inicio", type = "string", example = "2026-10-20T12:00:00", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime fin;

    @NotBlank(message = "El motivo es obligatorio")
    @Size(max = 200, message = "El motivo no puede superar 200 caracteres")
    @Schema(description = "Motivo del bloqueo", example = "Congreso de ortodoncia", maxLength = 200, requiredMode = Schema.RequiredMode.REQUIRED)
    private String motivo;
}
