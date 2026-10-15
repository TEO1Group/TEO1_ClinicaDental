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

@Entity
@Table(name = "bloqueo_doctor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BloqueoDoctor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id_bloqueo", updatable = false, nullable = false)
    private UUID id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "id_doctor", nullable = false)
    private Doctor doctor;

    @Column(name = "inicio", nullable = false)
    private LocalDateTime inicio;

    @Column(name = "fin", nullable = false)
    private LocalDateTime fin;

    @Column(name = "motivo", nullable = false, length = 200)
    private String motivo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creado_por")
    private Usuario creadoPor;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;
}
