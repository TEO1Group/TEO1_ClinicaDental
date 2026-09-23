package com.teo1.clinicadental.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ListaNegraRequest {

    @NotNull
    private Boolean enListaNegra;

    @Size(max = 200)
    private String motivo;
}
