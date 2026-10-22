package com.teo1.clinicadental.controller;

import com.teo1.clinicadental.dto.ErrorResponse;
import com.teo1.clinicadental.dto.ReporteHorasDemandadasResponse;
import com.teo1.clinicadental.dto.ReporteInasistenciasResponse;
import com.teo1.clinicadental.dto.ReporteIngresosTratamientoResponse;
import com.teo1.clinicadental.dto.ReporteCitasPorDoctorResponse;
import com.teo1.clinicadental.dto.ReporteGananciasResponse;
import com.teo1.clinicadental.service.ReporteCitaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
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
                    + " No cuenta citas cuya fecha y hora de inicio aun no llegan (hora de Guatemala); una cita pasada sin marcar sigue contando con su estado."
    )
    @ApiResponse(responseCode = "200", description = "Reporte generado")
    @ApiResponse(responseCode = "400", description = "Falta desde o hasta, alguna fecha no tiene formato yyyy-MM-dd o desde es posterior a hasta", content = @Content(
            mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class),
            examples = @ExampleObject(name = "fechasAusentes", summary = "Fechas requeridas", value = """
                    {"status":400,"error":"Bad Request","mensaje":"Debes indicar las fechas desde y hasta","path":"/reportes/citas-por-doctor"}
                    """)))
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
    @GetMapping("/horas-demandadas")
    @Operation(summary = "Reporte de horas mas demandadas",
            description = "Solo ADMIN y SECRETARIA. Cuenta citas de todos los estados y doctores por hora de inicio "
                    + "(09:00 y 09:30 se agrupan en la hora 9). Fechas desde y hasta inclusivas. "
                    + "Orden ascendente de hora; no incluye horas sin citas y devuelve una lista vacia si no hay datos."
                    + " No cuenta citas cuya fecha y hora de inicio aun no llegan (hora de Guatemala); una cita pasada sin marcar sigue contando con su estado.")
    @ApiResponse(responseCode = "200", description = "Cantidad de citas por hora")
    @ApiResponse(responseCode = "400", description = "Fechas ausentes, invalidas o rango invertido", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Falta autenticacion valida", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Solo ADMIN y SECRETARIA", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<ReporteHorasDemandadasResponse> horasDemandadas(
            @Parameter(description = "Primer dia incluido", example = "2026-10-01", required = true)
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @Parameter(description = "Ultimo dia incluido", example = "2026-10-31", required = true)
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(reporteCitaService.horasDemandadas(desde, hasta));
    }

    @GetMapping("/inasistencias")
    @Operation(summary = "Reporte de inasistencias y cancelaciones",
            description = "Solo ADMIN y SECRETARIA. Cuenta las citas NO_ASISTIO y CANCELADA cuya fecha esta entre desde y hasta, "
                    + "ambos incluidos, por doctor y por paciente, con los totales del periodo. Solo aparecen doctores y "
                    + "pacientes con alguna, activos o inactivos, ordenados de mayor a menor total y luego por apellido y nombre. Sin datos devuelve totales en cero y listas vacias."
                    + " No cuenta citas cuya fecha y hora de inicio aun no llegan (hora de Guatemala); una cita pasada sin marcar sigue contando con su estado.")
    @ApiResponse(responseCode = "200", description = "Inasistencias y cancelaciones del periodo")
    @ApiResponse(responseCode = "400", description = "Fechas ausentes, invalidas o rango invertido", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Falta autenticacion valida", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Solo ADMIN y SECRETARIA", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<ReporteInasistenciasResponse> inasistencias(
            @Parameter(description = "Primer dia incluido", example = "2026-10-01", required = true)
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @Parameter(description = "Ultimo dia incluido", example = "2026-10-31", required = true)
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(reporteCitaService.inasistencias(desde, hasta));
    }

    @GetMapping("/ingresos-por-tratamiento")
    @Operation(summary = "Reporte de ingresos por tratamiento",
            description = "Solo ADMIN y SECRETARIA. Por cada tratamiento registrado en citas cuya fecha esta entre desde y "
                    + "hasta, ambos incluidos, devuelve cuantas veces se realizo y la suma del costo cobrado (el costo guardado "
                    + "en la cita, no el del catalogo). Ordenado de mayor a menor ingreso; omite tratamientos sin registros. No incluye abonos de planes y el filtro usa la fecha de la cita, no la fecha de registro del tratamiento. Sin datos devuelve totales en cero y lista vacia."
                    + " No cuenta citas cuya fecha y hora de inicio aun no llegan (hora de Guatemala); una cita pasada sin marcar sigue contando con su estado.")
    @ApiResponse(responseCode = "200", description = "Ingresos por tratamiento del periodo")
    @ApiResponse(responseCode = "400", description = "Fechas ausentes, invalidas o rango invertido", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "401", description = "Falta autenticacion valida", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Solo ADMIN y SECRETARIA", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<ReporteIngresosTratamientoResponse> ingresosPorTratamiento(
            @Parameter(description = "Primer dia incluido", example = "2026-10-01", required = true)
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @Parameter(description = "Ultimo dia incluido", example = "2026-10-31", required = true)
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(reporteCitaService.ingresosPorTratamiento(desde, hasta));
    }

    @GetMapping("/ganancias")
    @Operation(summary = "Reporte de ganancias",
            description = "Solo ADMIN y SECRETARIA; DOCTOR y CLIENTE no. Ganancias son ingresos: no hay costos en la base. "
                    + "totalTratamientos y el total de cada doctor suman el costo cobrado en tratamientos de citas ATENDIDA cuya "
                    + "fecha esta entre desde y hasta, ambos incluidos. totalAbonos suma los abonos a planes registrados en el "
                    + "periodo y se informa aparte: no se suma a totalTratamientos para no contar dos veces un plan. Incluye a "
                    + "los doctores activos con ceros si no tienen ingresos y a los inactivos que si los tuvieron, ordenados por "
                    + "apellido y nombre."
                    + " No cuenta citas cuya fecha y hora de inicio aun no llegan (hora de Guatemala); una cita pasada sin marcar sigue contando con su estado.")
    @ApiResponse(responseCode = "200", description = "Ganancias del periodo")
    @ApiResponse(responseCode = "400", description = "Falta desde o hasta, alguna fecha no tiene formato yyyy-MM-dd o desde es posterior a hasta", content = @Content(
            mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class),
            examples = @ExampleObject(name = "rangoInvertido", summary = "Rango invertido", value = """
                    {"status":400,"error":"Bad Request","mensaje":"La fecha desde no puede ser posterior a hasta","path":"/reportes/ganancias"}
                    """)))
    @ApiResponse(responseCode = "401", description = "Falta autenticacion valida", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    @ApiResponse(responseCode = "403", description = "Se requiere el rol ADMIN o SECRETARIA", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    public ResponseEntity<ReporteGananciasResponse> ganancias(
            @Parameter(description = "Primer dia del periodo en formato yyyy-MM-dd, incluido", example = "2026-10-01", required = true)
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @Parameter(description = "Ultimo dia del periodo en formato yyyy-MM-dd, incluido", example = "2026-10-31", required = true)
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(reporteCitaService.ganancias(desde, hasta));
    }

}
