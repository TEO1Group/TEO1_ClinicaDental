package com.teo1.clinicadental.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Datos para crear un plan de tratamiento de varias sesiones")
public class PlanTratamientoRequest {

    @NotNull(message = "El paciente es obligatorio")
    @Schema(description = "Identificador del paciente", example = "8a2b3c4d-5e6f-4a1b-8c2d-3e4f5a6b7c8d", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID idCliente;

    @Schema(description = "Identificador del doctor. Obligatorio para ADMIN y SECRETARIA; un DOCTOR crea planes solo para si mismo y puede omitirlo", example = "1d2e3f4a-5b6c-4d7e-8f9a-0b1c2d3e4f5a")
    private UUID idDoctor;

    @NotNull(message = "El tratamiento es obligatorio")
    @Schema(description = "Identificador de un tratamiento activo del catalogo", example = "4e5f6a7b-8c9d-4e0f-9a1b-2c3d4e5f6a7b", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID idTratamiento;

    @NotNull(message = "El total de sesiones es obligatorio")
    @Min(value = 1, message = "El plan debe tener al menos 1 sesion")
    @Max(value = 50, message = "El plan no puede tener mas de 50 sesiones")
    @Schema(description = "Numero de sesiones del plan", example = "6", minimum = "1", maximum = "50", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer totalSesiones;

    @DecimalMin(value = "0.00", message = "El costo total no puede ser negativo")
    @Digits(integer = 8, fraction = 2, message = "El costo total debe tener como maximo 8 enteros y 2 decimales")
    @Schema(description = "Costo total del plan en quetzales; si se omite es el costo del catalogo por el numero de sesiones", example = "1800.00")
    private BigDecimal costoTotal;
}
