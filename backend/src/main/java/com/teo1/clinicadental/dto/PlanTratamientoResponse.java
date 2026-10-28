package com.teo1.clinicadental.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.teo1.clinicadental.model.EstadoPlan;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "Plan de tratamiento con su avance y su saldo")
public class PlanTratamientoResponse {

    @Schema(description = "Identificador del plan", example = "2b3c4d5e-6f7a-4b8c-9d0e-1f2a3b4c5d6e")
    private UUID idPlan;

    @Schema(description = "Identificador del paciente", example = "8a2b3c4d-5e6f-4a1b-8c2d-3e4f5a6b7c8d")
    private UUID idCliente;

    @Schema(description = "Nombre del paciente", example = "Ana")
    private String nombreCliente;

    @Schema(description = "Apellido del paciente", example = "Lopez")
    private String apellidoCliente;

    @Schema(description = "Identificador del doctor", example = "1d2e3f4a-5b6c-4d7e-8f9a-0b1c2d3e4f5a")
    private UUID idDoctor;

    @Schema(description = "Nombre del doctor", example = "Carlos")
    private String nombreDoctor;

    @Schema(description = "Apellido del doctor", example = "Perez")
    private String apellidoDoctor;

    @Schema(description = "Identificador del tratamiento", example = "4e5f6a7b-8c9d-4e0f-9a1b-2c3d4e5f6a7b")
    private UUID idTratamiento;

    @Schema(description = "Nombre del tratamiento", example = "Endodoncia")
    private String nombreTratamiento;

    @Schema(description = "ACTIVO, COMPLETADO (todas las sesiones atendidas) o CANCELADO", example = "ACTIVO")
    private EstadoPlan estado;

    @Schema(description = "Fecha y hora de creacion del plan", type = "string", example = "2026-10-21T10:15:00")
    private LocalDateTime fechaCreacion;

    @Schema(description = "Numero de sesiones del plan", example = "6")
    private int totalSesiones;

    @Schema(description = "Sesiones con la cita ATENDIDA", example = "2")
    private long sesionesAtendidas;

    @Schema(description = "Sesiones con la cita AGENDADA, todavia sin atender", example = "1")
    private long sesionesAgendadas;

    @Schema(description = "Sesiones que faltan por agendar (las canceladas y las inasistencias se pueden volver a agendar)", example = "3")
    private long sesionesPendientes;

    @Schema(description = "Avance: sesiones atendidas sobre sesiones totales, de 0 a 100", example = "33")
    private int porcentajeAvance;

    @Schema(description = "Costo total del plan en quetzales", example = "1800.00")
    private BigDecimal costoTotal;

    @Schema(description = "Suma de los abonos registrados", example = "600.00")
    private BigDecimal totalAbonado;

    @Schema(description = "Costo total menos los abonos", example = "1200.00")
    private BigDecimal saldo;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Citas vinculadas al plan, en orden de fecha; solo en el detalle de un plan", nullable = true)
    private List<CitaResponse> citas;
}
