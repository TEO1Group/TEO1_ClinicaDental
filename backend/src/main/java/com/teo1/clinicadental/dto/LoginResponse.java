package com.teo1.clinicadental.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "JWT para usar en Authorize; pegar solo el token, sin el prefijo Bearer")
public class LoginResponse {

    @Schema(description = "Token JWT firmado devuelto al iniciar sesión", example = "<JWT devuelto por el servidor>")
    private String token;
}
