package com.teo1.clinicadental.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Inclusión o retiro de lista negra; al retirar se borra el motivo")
public class ListaNegraRequest {

    @NotNull
    @Schema(description = "true para incluir; false para retirar", example = "true")
    private Boolean enListaNegra;

    @Size(max = 200)
    @Schema(description = "Obligatorio y no vacío al incluir; se ignora al retirar", example = "Ausencias reiteradas", maxLength = 200)
    private String motivo;
}
