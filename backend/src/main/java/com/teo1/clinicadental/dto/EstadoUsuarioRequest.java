package com.teo1.clinicadental.dto;

import com.teo1.clinicadental.model.EstadoUsuario;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class EstadoUsuarioRequest {

    @NotNull(message = "El estado es obligatorio")
    private EstadoUsuario estado;
}
