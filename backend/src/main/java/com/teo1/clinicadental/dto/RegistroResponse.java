package com.teo1.clinicadental.dto;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RegistroResponse {

    private String mensaje;
    private UUID id;
}
