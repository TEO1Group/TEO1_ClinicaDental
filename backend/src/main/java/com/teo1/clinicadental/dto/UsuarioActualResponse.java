package com.teo1.clinicadental.dto;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UsuarioActualResponse {

    private UUID idUsuario;
    private String nombre;
    private String apellido;
    private String email;
    private String telefono;
    private String rol;
    private UUID idPerfil;
}
