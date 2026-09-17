package com.teo1.clinicadental.dto;

import java.time.LocalDate;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ClienteResponse {

    private UUID idCliente;
    private UUID idUsuario;
    private String nombre;
    private String apellido;
    private String email;
    private String telefono;
    private String dpi;
    private String direccion;
    private LocalDate fechaNacimiento;
    private boolean enListaNegra;
    private String motivoListaNegra;
}
