package com.teo1.clinicadental.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Abono al plan; fecha y usuario se obtienen del servidor")
public class AbonoPlanRequest {

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0", inclusive = false, message = "El monto debe ser mayor que cero")
    @Digits(integer = 8, fraction = 2, message = "El monto admite hasta 8 enteros y 2 decimales")
    @Schema(description = "Monto en quetzales, positivo y no superior al saldo", example = "150.50",
            minimum = "0", exclusiveMinimum = true, requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal monto;
}
