package com.teo1.clinicadental.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "Version del backend desplegado. Los dos campos vienen del build de la imagen; son null en un build local sin esos datos")
public class VersionResponse {

    @Schema(description = "Hash corto (7 caracteres) del commit con el que se construyo la imagen", nullable = true, example = "dc45780")
    private String commit;

    @Schema(description = "Fecha y hora de construccion de la imagen en UTC", type = "string", format = "date-time", nullable = true, example = "2026-10-28T16:22:12Z")
    private Instant fechaConstruccion;
}
