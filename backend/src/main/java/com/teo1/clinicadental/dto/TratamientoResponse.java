package com.teo1.clinicadental.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "Tratamiento del catalogo de la clinica")
public class TratamientoResponse {

    @Schema(description = "Identificador del tratamiento", example = "2a3b4c5d-6e7f-4a8b-9c0d-1e2f3a4b5c6d")
    private UUID idTratamiento;

    @Schema(description = "Nombre del tratamiento", example = "Limpieza dental")
    private String nombre;

    @Schema(description = "Descripcion del tratamiento", example = "Profilaxis: remocion de placa y sarro", nullable = true)
    private String descripcion;

    @Schema(description = "Duracion estimada en minutos", example = "45")
    private Integer duracionMinutos;

    @Schema(description = "Costo sugerido en quetzales", example = "300.00")
    private BigDecimal costo;

    @Schema(description = "Disponible para nuevos registros; desactivarlo conserva el historial", example = "true")
    private boolean activo;
}
