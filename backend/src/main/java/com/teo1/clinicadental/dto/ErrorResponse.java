package com.teo1.clinicadental.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Error de la API. timestamp y errores pueden omitirse; los errores de seguridad no los incluyen")
public class ErrorResponse {

    @Schema(description = "Fecha del error, cuando la genera el manejador del controller", example = "2026-10-15T10:00:00")
    private LocalDateTime timestamp;
    @Schema(description = "Código HTTP de la respuesta")
    private int status;
    @Schema(description = "Nombre del estado HTTP de la respuesta")
    private String error;
    @Schema(description = "Detalle del error")
    private String mensaje;
    @Schema(description = "Ruta de la petición")
    private String path;
    @Schema(description = "Errores de validación por campo; se omite en otros errores")
    private Map<String, String> errores;
}
