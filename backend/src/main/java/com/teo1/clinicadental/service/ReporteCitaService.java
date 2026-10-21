package com.teo1.clinicadental.service;

import com.teo1.clinicadental.dto.CitasPorDoctorResponse;
import com.teo1.clinicadental.dto.ReporteHorasDemandadasResponse;
import com.teo1.clinicadental.dto.ReporteCitasPorDoctorResponse;
import com.teo1.clinicadental.model.Doctor;
import com.teo1.clinicadental.model.EstadoCita;
import com.teo1.clinicadental.model.EstadoUsuario;
import com.teo1.clinicadental.repository.DoctorRepository;
import com.teo1.clinicadental.repository.ReporteCitaRepository;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ReporteCitaService {

    private final ReporteCitaRepository reporteCitaRepository;
    private final DoctorRepository doctorRepository;

    @Transactional(readOnly = true)
    public ReporteCitasPorDoctorResponse citasPorDoctor(LocalDate desde, LocalDate hasta) {
        validarPeriodo(desde, hasta);

        Map<UUID, Map<EstadoCita, Long>> conteos = new HashMap<>();
        for (ReporteCitaRepository.ConteoCitas conteo : reporteCitaRepository.contarPorDoctorYEstado(desde, hasta)) {
            conteos.computeIfAbsent(conteo.getIdDoctor(), id -> new EnumMap<>(EstadoCita.class))
                    .merge(conteo.getEstado(), conteo.getCantidad(), Long::sum);
        }

        List<CitasPorDoctorResponse> doctores = doctorRepository.findByUsuarioEstado(EstadoUsuario.ACTIVO).stream()
                .sorted(Comparator
                        .comparing((Doctor doctor) -> doctor.getUsuario().getApellido(), String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(doctor -> doctor.getUsuario().getNombre(), String.CASE_INSENSITIVE_ORDER))
                .map(doctor -> toResponse(doctor, conteos.getOrDefault(doctor.getId(), Map.of())))
                .toList();

        return new ReporteCitasPorDoctorResponse(desde, hasta, doctores);
    }

    @Transactional(readOnly = true)
    public ReporteHorasDemandadasResponse horasDemandadas(LocalDate desde, LocalDate hasta) {
        validarPeriodo(desde, hasta);
        return new ReporteHorasDemandadasResponse(desde, hasta,
                reporteCitaRepository.contarPorHora(desde, hasta).stream()
                        .map(conteo -> new ReporteHorasDemandadasResponse.HoraDemandada(conteo.getHora(), conteo.getCantidad()))
                        .toList());
    }

    private void validarPeriodo(LocalDate desde, LocalDate hasta) {
        if (desde == null || hasta == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debes indicar las fechas desde y hasta");
        }
        if (desde.isAfter(hasta)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La fecha desde no puede ser posterior a hasta");
        }
    }

    private CitasPorDoctorResponse toResponse(Doctor doctor, Map<EstadoCita, Long> porEstado) {
        long agendadas = porEstado.getOrDefault(EstadoCita.AGENDADA, 0L);
        long atendidas = porEstado.getOrDefault(EstadoCita.ATENDIDA, 0L);
        long canceladas = porEstado.getOrDefault(EstadoCita.CANCELADA, 0L);
        long noAsistio = porEstado.getOrDefault(EstadoCita.NO_ASISTIO, 0L);

        return new CitasPorDoctorResponse(
                doctor.getId(),
                doctor.getUsuario().getNombre(),
                doctor.getUsuario().getApellido(),
                doctor.getEspecialidad(),
                agendadas,
                atendidas,
                canceladas,
                noAsistio,
                agendadas + atendidas + canceladas + noAsistio
        );
    }
}
