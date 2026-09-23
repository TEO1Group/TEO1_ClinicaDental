package com.teo1.clinicadental.dto;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DoctorResponse {

    private UUID idDoctor;
    private UUID idUsuario;
    private String nombre;
    private String apellido;
    private String email;
    private String telefono;
    private String especialidad;
    private String numeroColegiado;
    private String numeroClinica;
    private BigDecimal calificacionPromedio;
    private String estado;
}
