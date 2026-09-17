package com.teo1.clinicadental.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DoctorUpdateRequest {

    @NotBlank
    private String especialidad;

    private String numeroColegiado;

    private String numeroClinica;
}
