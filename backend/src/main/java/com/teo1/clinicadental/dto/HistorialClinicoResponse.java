package com.teo1.clinicadental.dto;

import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class HistorialClinicoResponse {

    private UUID idHistorial;
    private UUID idCliente;
    private LocalDateTime fecha;
    private String descripcion;
}
