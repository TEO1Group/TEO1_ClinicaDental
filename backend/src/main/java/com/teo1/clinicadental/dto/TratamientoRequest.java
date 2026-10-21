package com.teo1.clinicadental.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Creacion o reemplazo del tratamiento; activo vale true si se omite")
public class TratamientoRequest {
    @NotBlank
    @Size(max = 100)
    @Schema(description = "Nombre unico, se recortan espacios exteriores", example = "Corona de zirconio")
    private String nombre;

    @Schema(description = "Descripcion opcional", example = "Restauracion fija por pieza")
    private String descripcion;

    @NotNull
    @Positive
    @Schema(description = "Duracion estimada positiva en minutos", example = "60", minimum = "1")
    private Integer duracionMinutos;

    @NotNull
    @DecimalMin("0.00")
    @Digits(integer = 8, fraction = 2)
    @Schema(description = "Costo sugerido en quetzales, sin modificar costos ya cobrados", example = "2500.00", minimum = "0")
    private BigDecimal costo;

    @NotNull
    @Schema(description = "Disponible para nuevos registros; por defecto true", example = "true", defaultValue = "true")
    private Boolean activo = true;
}
