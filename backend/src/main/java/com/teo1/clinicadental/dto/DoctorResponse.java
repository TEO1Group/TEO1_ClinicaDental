package com.teo1.clinicadental.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "Perfil del doctor y estado de su cuenta")
public class DoctorResponse {

    @Schema(description = "Identificador del doctor", example = "1d2e3f4a-5b6c-4d7e-8f9a-0b1c2d3e4f5a")
    private UUID idDoctor;
    @Schema(description = "Identificador del usuario", example = "1d2e3f4a-5b6c-4d7e-8f9a-0b1c2d3e4f5a")
    private UUID idUsuario;
    @Schema(description = "Nombre", example = "Ana")
    private String nombre;
    @Schema(description = "Apellido", example = "Lopez")
    private String apellido;
    @Schema(description = "Correo electrónico", example = "ana@example.com")
    private String email;
    @Schema(description = "Teléfono de 8 dígitos", example = "55551234")
    private String telefono;
    @Schema(description = "Especialidad del doctor", example = "Ortodoncia")
    private String especialidad;
    @Schema(description = "Número de colegiado", example = "COL-1234")
    private String numeroColegiado;
    @Schema(description = "Número de clínica", example = "101")
    private String numeroClinica;
    @Schema(description = "Calificación promedio", example = "0.00")
    private BigDecimal calificacionPromedio;
    @Schema(description = "Estado de la cuenta", allowableValues = {"ACTIVO", "INACTIVO"}, example = "ACTIVO")
    private String estado;
}
