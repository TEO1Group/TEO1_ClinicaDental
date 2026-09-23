package com.teo1.clinicadental.dto;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RolResponse {

    private UUID idRol;
    private String nombreRol;
    private String descripcion;
}
