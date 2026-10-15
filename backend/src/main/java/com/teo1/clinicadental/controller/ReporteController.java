package com.teo1.clinicadental.controller;

import com.teo1.clinicadental.dto.ErrorResponse;
import com.teo1.clinicadental.dto.ReporteCitasPorDoctorResponse;
import com.teo1.clinicadental.service.ReporteCitaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reportes")
@RequiredArgsConstructor
@Tag(name = "Reportes", description = "Reportes de gestion de la clinica")
@SecurityRequirement(name = "bearerAuth")
public class ReporteController {

    private final ReporteCitaService reporteCitaService;

    @GetMapping("/citas-por-doctor")
    @Operation(
            summary = "Reporte de citas por doctor",
            description = "Requiere rol ADMIN o SECRETARIA; DOCTOR y CLIENTE no. Cuenta las citas de cada doctor por estado "
                    + "(AGENDADA, ATENDIDA, CANCELADA y NO_ASISTIO) cuya fecha esta entre desde y hasta, ambos incluidos. "
                    + "Incluye a todos los doctores activos, con ceros si no tienen citas en el periodo, ordenados por "
                    + "apellido y nombre. Las citas de doctores inactivos no aparecen."
    )
    @ApiResponse(responseCode = "200", description = "Reporte generado")
    @ApiResponse(responseCode = "400", description = "Falta desde o hasta, alguna fecha no tiene formato yyyy-MM-dd o desde es posterior a hasta", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Falta autenticacion valida", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Se requiere el rol ADMIN o SECRETARIA", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<ReporteCitasPorDoctorResponse> citasPorDoctor(
            @Parameter(description = "Primer dia del periodo en formato yyyy-MM-dd, incluido", example = "2026-10-01", required = true)
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @Parameter(description = "Ultimo dia del periodo en formato yyyy-MM-dd, incluido", example = "2026-10-31", required = true)
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta
    ) {
        return ResponseEntity.ok(reporteCitaService.citasPorDoctor(desde, hasta));
    }
}
