package com.teo1.clinicadental.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "Confirmación de la creación del usuario")
public class RegistroResponse {

    @Schema(description = "Confirmación", example = "Usuario registrado exitosamente")
    private String mensaje;
    @Schema(description = "Identificador del usuario creado", example = "1d2e3f4a-5b6c-4d7e-8f9a-0b1c2d3e4f5a")
    private UUID id;
}
