package com.teo1.clinicadental.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class HistorialClinicoRequest {

    @NotBlank
    private String descripcion;
}
