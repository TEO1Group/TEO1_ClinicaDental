package com.teo1.clinicadental.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.teo1.clinicadental.model.EstadoUsuario;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Cambio de estado de una cuenta")
public class EstadoUsuarioRequest {

    @NotNull(message = "El estado es obligatorio")
    @Schema(description = "Estado de la cuenta", allowableValues = {"ACTIVO", "INACTIVO"}, example = "ACTIVO")
    private EstadoUsuario estado;
}
