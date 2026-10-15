package com.teo1.clinicadental.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "Rol disponible en el sistema")
public class RolResponse {

    @Schema(description = "Identificador del rol", example = "1d2e3f4a-5b6c-4d7e-8f9a-0b1c2d3e4f5a")
    private UUID idRol;
    @Schema(description = "Rol de la cuenta", allowableValues = {"ADMIN", "SECRETARIA", "DOCTOR", "CLIENTE"}, example = "CLIENTE")
    private String nombreRol;
    @Schema(description = "Descripción del rol", example = "Paciente de la clínica")
    private String descripcion;
}
