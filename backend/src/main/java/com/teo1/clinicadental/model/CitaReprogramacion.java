package com.teo1.clinicadental.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "cita_reprogramacion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CitaReprogramacion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_reprogramacion", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cita", nullable = false)
    private Cita cita;

    @Column(name = "fecha_anterior", nullable = false)
    private LocalDate fechaAnterior;

    @Column(name = "hora_anterior", nullable = false)
    private LocalTime horaAnterior;

    @Column(name = "fecha_nueva", nullable = false)
    private LocalDate fechaNueva;

    @Column(name = "hora_nueva", nullable = false)
    private LocalTime horaNueva;

    @Column(name = "motivo", nullable = false, length = 200)
    private String motivo;

    @Column(name = "fecha_cambio", nullable = false, updatable = false)
    private LocalDateTime fechaCambio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reprogramado_por")
    private Usuario reprogramadoPor;
}
