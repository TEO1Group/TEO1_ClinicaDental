package com.teo1.clinicadental.controller;

import com.teo1.clinicadental.dto.VersionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Version", description = "Version desplegada del backend")
public class VersionController {

    private static final int LARGO_HASH_CORTO = 7;

    private final VersionResponse version;

    // Los valores entran como argumentos del build de la imagen (GIT_COMMIT y BUILD_DATE en el Dockerfile)
    public VersionController(
            @Value("${app.version.commit:}") String commit,
            @Value("${app.version.fecha-construccion:}") String fechaConstruccion) {
        this.version = new VersionResponse(hashCorto(commit), fecha(fechaConstruccion));
    }

    @GetMapping("/version")
    @Operation(summary = "Consultar la version desplegada", description = "Publica, sin token. Devuelve el hash corto del commit y la fecha de construccion de la imagen del backend. Ambos campos son null si la imagen se construyo sin esos datos (por ejemplo, en local).")
    @ApiResponse(responseCode = "200", description = "Version del backend")
    public ResponseEntity<VersionResponse> obtener() {
        return ResponseEntity.ok(version);
    }

    private static String hashCorto(String commit) {
        if (commit == null || commit.isBlank()) {
            return null;
        }
        String limpio = commit.strip();
        return limpio.length() > LARGO_HASH_CORTO ? limpio.substring(0, LARGO_HASH_CORTO) : limpio;
    }

    private static Instant fecha(String fechaConstruccion) {
        if (fechaConstruccion == null || fechaConstruccion.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(fechaConstruccion.strip());
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
