package com.teo1.clinicadental.service;

import com.teo1.clinicadental.dto.CitasPorDoctorResponse;
import com.teo1.clinicadental.dto.ReporteHorasDemandadasResponse;
import com.teo1.clinicadental.dto.ReporteInasistenciasResponse;
import com.teo1.clinicadental.dto.ReporteIngresosTratamientoResponse;
import com.teo1.clinicadental.dto.ReporteCitasPorDoctorResponse;
import com.teo1.clinicadental.dto.ReporteGananciasResponse;
import com.teo1.clinicadental.model.Doctor;
import com.teo1.clinicadental.model.EstadoCita;
import com.teo1.clinicadental.model.EstadoUsuario;
import com.teo1.clinicadental.repository.DoctorRepository;
import com.teo1.clinicadental.repository.ReporteCitaRepository;
import com.teo1.clinicadental.repository.ReporteGananciaRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
    private final ReporteGananciaRepository reporteGananciaRepository;
    private final DoctorRepository doctorRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public ReporteCitasPorDoctorResponse citasPorDoctor(LocalDate desde, LocalDate hasta) {
        validarPeriodo(desde, hasta);
        LocalDateTime ahora = LocalDateTime.now(clock);

        Map<UUID, Map<EstadoCita, Long>> conteos = new HashMap<>();
        for (ReporteCitaRepository.ConteoCitas conteo : reporteCitaRepository.contarPorDoctorYEstado(
                desde, hasta, ahora.toLocalDate(), ahora.toLocalTime())) {
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
        LocalDateTime ahora = LocalDateTime.now(clock);
        return new ReporteHorasDemandadasResponse(desde, hasta,
                reporteCitaRepository.contarPorHora(desde, hasta, ahora.toLocalDate(), ahora.toLocalTime()).stream()
                        .map(conteo -> new ReporteHorasDemandadasResponse.HoraDemandada(conteo.getHora(), conteo.getCantidad()))
                        .toList());
    }

    // Una sola consulta agrupada por doctor, paciente y estado; aqui solo se suma por doctor y por paciente
    @Transactional(readOnly = true)
    public ReporteInasistenciasResponse inasistencias(LocalDate desde, LocalDate hasta) {
        validarPeriodo(desde, hasta);
        LocalDateTime ahora = LocalDateTime.now(clock);

        Map<UUID, Fila> porDoctor = new LinkedHashMap<>();
        Map<UUID, Fila> porPaciente = new LinkedHashMap<>();
        long totalNoAsistio = 0;
        long totalCanceladas = 0;

        for (ReporteCitaRepository.ConteoPorDoctorYPaciente conteo : reporteCitaRepository.contarPorDoctorYPaciente(
                desde, hasta, List.of(EstadoCita.NO_ASISTIO, EstadoCita.CANCELADA),
                ahora.toLocalDate(), ahora.toLocalTime())) {
            boolean noAsistio = conteo.getEstado() == EstadoCita.NO_ASISTIO;
            porDoctor.computeIfAbsent(conteo.getIdDoctor(),
                            id -> new Fila(id, conteo.getNombreDoctor(), conteo.getApellidoDoctor()))
                    .sumar(noAsistio, conteo.getCantidad());
            porPaciente.computeIfAbsent(conteo.getIdCliente(),
                            id -> new Fila(id, conteo.getNombreCliente(), conteo.getApellidoCliente()))
                    .sumar(noAsistio, conteo.getCantidad());
            if (noAsistio) {
                totalNoAsistio += conteo.getCantidad();
            } else {
                totalCanceladas += conteo.getCantidad();
            }
        }

        return new ReporteInasistenciasResponse(desde, hasta, totalNoAsistio, totalCanceladas,
                totalNoAsistio + totalCanceladas,
                ordenar(porDoctor.values()).stream()
                        .map(fila -> new ReporteInasistenciasResponse.PorDoctor(fila.id, fila.nombre, fila.apellido,
                                fila.noAsistio, fila.canceladas, fila.total()))
                        .toList(),
                ordenar(porPaciente.values()).stream()
                        .map(fila -> new ReporteInasistenciasResponse.PorPaciente(fila.id, fila.nombre, fila.apellido,
                                fila.noAsistio, fila.canceladas, fila.total()))
                        .toList());
    }

    @Transactional(readOnly = true)
    public ReporteIngresosTratamientoResponse ingresosPorTratamiento(LocalDate desde, LocalDate hasta) {
        validarPeriodo(desde, hasta);
        LocalDateTime ahora = LocalDateTime.now(clock);

        List<ReporteIngresosTratamientoResponse.IngresoTratamiento> tratamientos =
                reporteCitaRepository.sumarIngresosPorTratamiento(
                        desde, hasta, ahora.toLocalDate(), ahora.toLocalTime()).stream()
                        .map(ingreso -> new ReporteIngresosTratamientoResponse.IngresoTratamiento(
                                ingreso.getIdTratamiento(), ingreso.getNombre(), ingreso.getCantidad(), ingreso.getTotal()))
                        .toList();

        return new ReporteIngresosTratamientoResponse(desde, hasta,
                tratamientos.stream().mapToLong(ReporteIngresosTratamientoResponse.IngresoTratamiento::cantidad).sum(),
                tratamientos.stream().map(ReporteIngresosTratamientoResponse.IngresoTratamiento::total)
                        .reduce(BigDecimal.ZERO, BigDecimal::add),
                tratamientos);
    }

    @Transactional(readOnly = true)
    public ReporteGananciasResponse ganancias(LocalDate desde, LocalDate hasta) {
        validarPeriodo(desde, hasta);
        LocalDateTime ahora = LocalDateTime.now(clock);

        Map<UUID, ReporteGananciasResponse.GananciaDoctor> porDoctor = new HashMap<>();
        for (ReporteGananciaRepository.IngresoDoctor ingreso
                : reporteGananciaRepository.sumarTratamientosPorDoctor(
                desde, hasta, EstadoCita.ATENDIDA, ahora.toLocalDate(), ahora.toLocalTime())) {
            porDoctor.put(ingreso.getIdDoctor(), new ReporteGananciasResponse.GananciaDoctor(ingreso.getIdDoctor(),
                    ingreso.getNombreDoctor(), ingreso.getApellidoDoctor(), ingreso.getCantidad(), ingreso.getTotal()));
        }
        for (Doctor doctor : doctorRepository.findByUsuarioEstado(EstadoUsuario.ACTIVO)) {
            porDoctor.putIfAbsent(doctor.getId(), new ReporteGananciasResponse.GananciaDoctor(doctor.getId(),
                    doctor.getUsuario().getNombre(), doctor.getUsuario().getApellido(), 0, BigDecimal.ZERO));
        }

        List<ReporteGananciasResponse.GananciaDoctor> doctores = porDoctor.values().stream()
                .sorted(Comparator
                        .comparing(ReporteGananciasResponse.GananciaDoctor::apellidoDoctor, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(ReporteGananciasResponse.GananciaDoctor::nombreDoctor, String.CASE_INSENSITIVE_ORDER))
                .toList();

        BigDecimal totalTratamientos = doctores.stream()
                .map(ReporteGananciasResponse.GananciaDoctor::total)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalAbonos = reporteGananciaRepository.sumarAbonos(
                desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay());

        return new ReporteGananciasResponse(desde, hasta, totalTratamientos,
                totalAbonos != null ? totalAbonos : BigDecimal.ZERO, doctores);
    }

    private List<Fila> ordenar(Collection<Fila> filas) {
        return filas.stream()
                .sorted(Comparator.comparingLong(Fila::total).reversed()
                        .thenComparing((Fila fila) -> fila.apellido, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(fila -> fila.nombre, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private static final class Fila {

        private final UUID id;
        private final String nombre;
        private final String apellido;
        private long noAsistio;
        private long canceladas;

        private Fila(UUID id, String nombre, String apellido) {
            this.id = id;
            this.nombre = nombre;
            this.apellido = apellido;
        }

        private void sumar(boolean esNoAsistio, long cantidad) {
            if (esNoAsistio) {
                noAsistio += cantidad;
            } else {
                canceladas += cantidad;
            }
        }

        private long total() {
            return noAsistio + canceladas;
        }
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
