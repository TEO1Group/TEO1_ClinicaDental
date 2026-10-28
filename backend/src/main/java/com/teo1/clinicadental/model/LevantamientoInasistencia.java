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
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Levantamiento de la restriccion por inasistencias de un paciente. Las inasistencias se cuentan
 * desde la {@code fecha} del ultimo levantamiento.
 */
@Entity
@Table(name = "levantamiento_inasistencia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LevantamientoInasistencia {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_levantamiento", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;

    @Column(name = "motivo", nullable = false, length = 200)
    private String motivo;

    // Inasistencias que tenia el paciente al levantarle la restriccion
    @Column(name = "inasistencias", nullable = false)
    private int inasistencias;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "levantado_por")
    private Usuario levantadoPor;

    @Column(name = "fecha", nullable = false, updatable = false)
    private LocalDateTime fecha;
}
