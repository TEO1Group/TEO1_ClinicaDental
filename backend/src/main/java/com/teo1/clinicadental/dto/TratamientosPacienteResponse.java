package com.teo1.clinicadental.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "Tratamientos registrados en las citas de un paciente y su costo total")
public class TratamientosPacienteResponse {

    @Schema(description = "Suma de los costos cobrados en quetzales", example = "650.00")
    private BigDecimal total;

    @Schema(description = "Tratamientos del paciente, de la cita mas reciente a la mas antigua")
    private List<CitaTratamientoResponse> tratamientos;
}
