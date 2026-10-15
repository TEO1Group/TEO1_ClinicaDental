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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "cita_tratamiento")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CitaTratamiento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_cita_tratamiento", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_cita", nullable = false)
    private Cita cita;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tratamiento", nullable = false)
    private Tratamiento tratamiento;

    @Column(name = "costo", nullable = false, precision = 10, scale = 2)
    private BigDecimal costo;

    @Column(name = "observaciones")
    private String observaciones;

    @Column(name = "fecha_registro", nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;
}
