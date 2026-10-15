package com.teo1.clinicadental.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "Paciente y estado de su cuenta")
public class ClienteResponse {

    @Schema(description = "Identificador del paciente", example = "1d2e3f4a-5b6c-4d7e-8f9a-0b1c2d3e4f5a")
    private UUID idCliente;
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
    @Schema(description = "DPI de 13 dígitos", example = "1234567890123")
    private String dpi;
    @Schema(description = "Dirección", example = "Zona 1, Ciudad de Guatemala")
    private String direccion;
    @Schema(description = "Fecha de nacimiento, anterior a hoy", example = "1995-04-12")
    private LocalDate fechaNacimiento;
    @Schema(description = "Indica si el paciente está en lista negra", example = "false")
    private boolean enListaNegra;
    @Schema(description = "Motivo; null cuando no está en lista negra", example = "Ausencias reiteradas", nullable = true)
    private String motivoListaNegra;
    @Schema(description = "Estado de la cuenta", allowableValues = {"ACTIVO", "INACTIVO"}, example = "ACTIVO")
    private String estado;
}
