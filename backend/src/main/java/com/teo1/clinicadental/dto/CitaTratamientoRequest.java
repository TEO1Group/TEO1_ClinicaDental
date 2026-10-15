package com.teo1.clinicadental.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Tratamiento realizado en una cita")
public class CitaTratamientoRequest {

    @NotNull(message = "El tratamiento es obligatorio")
    @Schema(description = "Identificador del tratamiento del catalogo", example = "2a3b4c5d-6e7f-4a8b-9c0d-1e2f3a4b5c6d", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID idTratamiento;

    @DecimalMin(value = "0.00", message = "El costo no puede ser negativo")
    @Digits(integer = 8, fraction = 2, message = "El costo admite hasta 8 enteros y 2 decimales")
    @Schema(description = "Costo cobrado en quetzales. Si no se envia se usa el costo del catalogo", example = "250.00", minimum = "0")
    private BigDecimal costo;

    @Size(max = 1000, message = "Las observaciones no pueden superar 1000 caracteres")
    @Schema(description = "Observaciones del doctor", example = "Pieza 36, sin complicaciones", maxLength = 1000)
    private String observaciones;
}
