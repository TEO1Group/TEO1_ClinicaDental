package com.teo1.clinicadental.service;

import com.teo1.clinicadental.model.BloqueoDoctor;
import com.teo1.clinicadental.model.Cita;
import com.teo1.clinicadental.model.DiaSemana;
import com.teo1.clinicadental.model.Doctor;
import com.teo1.clinicadental.model.EstadoCita;
import com.teo1.clinicadental.model.Horario;
import com.teo1.clinicadental.repository.BloqueoDoctorRepository;
import com.teo1.clinicadental.repository.CitaRepository;
import com.teo1.clinicadental.repository.HorarioRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DisponibilidadCitaServiceTests {

    private static final ZoneId ZONA = ZoneId.of("America/Guatemala");
    private static final LocalDate FECHA = LocalDate.of(2026, 10, 15);
    private static final DiaSemana DIA = DiaSemana.desde(FECHA.getDayOfWeek());

    @Mock
    private CitaRepository citaRepository;

    @Mock
    private HorarioRepository horarioRepository;

    @Mock
    private BloqueoDoctorRepository bloqueoDoctorRepository;

    private DisponibilidadCitaService disponibilidadCitaService;
    private Doctor doctor;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(ZonedDateTime.of(2026, 10, 8, 8, 0, 0, 0, ZONA).toInstant(), ZONA);
        disponibilidadCitaService = new DisponibilidadCitaService(
                citaRepository, horarioRepository, bloqueoDoctorRepository, clock, 30, 24);
        doctor = Doctor.builder()
                .id(UUID.randomUUID())
                .especialidad("Odontologia general")
                .build();
    }

    @Test
    void citaLibreDentroDelHorarioSeAcepta() {
        prepararCitas(List.of());

        assertDoesNotThrow(() -> disponibilidadCitaService.validar(doctor, FECHA, LocalTime.of(9, 0), null));
    }

    @Test
    void excluirLaPropiaCitaPermiteMoverlaSinChocarConsigoMisma() {
        Cita propia = cita(LocalTime.of(9, 0));
        prepararCitas(List.of(propia));

        assertDoesNotThrow(() -> disponibilidadCitaService.validar(doctor, FECHA, LocalTime.of(9, 15), propia.getId()));
    }

    @Test
    void sinExcluirLaPropiaCitaChocaConsigoMisma() {
        Cita propia = cita(LocalTime.of(9, 0));
        prepararCitas(List.of(propia));

        assertEstado(HttpStatus.CONFLICT, () -> disponibilidadCitaService.validar(doctor, FECHA, LocalTime.of(9, 15), null));
    }

    @Test
    void laCitaExcluidaNoTapaUnChoqueConOtraCita() {
        Cita propia = cita(LocalTime.of(9, 0));
        Cita otra = cita(LocalTime.of(10, 0));
        prepararCitas(List.of(propia, otra));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> disponibilidadCitaService.validar(doctor, FECHA, LocalTime.of(10, 15), propia.getId()));
        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals(DisponibilidadCitaService.MENSAJE_SOLAPE, exception.getReason());
    }

    @Test
    void citaQueTerminaJustoCuandoEmpiezaOtraSeAcepta() {
        prepararCitas(List.of(cita(LocalTime.of(10, 0))));

        assertDoesNotThrow(() -> disponibilidadCitaService.validar(doctor, FECHA, LocalTime.of(9, 30), null));
    }

    @Test
    void citaQueEmpiezaJustoCuandoTerminaOtraSeAcepta() {
        prepararCitas(List.of(cita(LocalTime.of(9, 0))));

        assertDoesNotThrow(() -> disponibilidadCitaService.validar(doctor, FECHA, LocalTime.of(9, 30), null));
    }

    @Test
    void citaFueraDelHorarioDevuelveConflicto() {
        prepararHorario();

        assertEstado(HttpStatus.CONFLICT, () -> disponibilidadCitaService.validar(doctor, FECHA, LocalTime.of(11, 45), null));
    }

    @Test
    void fechaPasadaDevuelveSolicitudInvalidaSinConsultarRepositorios() {
        assertEstado(HttpStatus.BAD_REQUEST,
                () -> disponibilidadCitaService.validar(doctor, LocalDate.of(2026, 10, 8), LocalTime.of(8, 0), null));
        verifyNoInteractions(horarioRepository, citaRepository);
    }

    @Test
    void citaDentroDeUnBloqueoDevuelveConflictoConElMotivo() {
        prepararHorario();
        prepararBloqueo(LocalTime.of(10, 0), LocalTime.of(11, 0));

        for (LocalTime hora : List.of(LocalTime.of(10, 0), LocalTime.of(9, 45), LocalTime.of(10, 45))) {
            ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                    () -> disponibilidadCitaService.validar(doctor, FECHA, hora, null));
            assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
            assertEquals("El doctor no atiende en ese horario: Congreso de ortodoncia", exception.getReason());
        }
        verifyNoInteractions(citaRepository);
    }

    @Test
    void citaQueTerminaJustoCuandoEmpiezaElBloqueoSeAcepta() {
        prepararCitas(List.of());
        prepararBloqueo(LocalTime.of(10, 0), LocalTime.of(11, 0));

        assertDoesNotThrow(() -> disponibilidadCitaService.validar(doctor, FECHA, LocalTime.of(9, 30), null));
    }

    @Test
    void citaQueEmpiezaJustoCuandoTerminaElBloqueoSeAcepta() {
        prepararCitas(List.of());
        prepararBloqueo(LocalTime.of(10, 0), LocalTime.of(11, 0));

        assertDoesNotThrow(() -> disponibilidadCitaService.validar(doctor, FECHA, LocalTime.of(11, 0), null));
    }

    @Test
    void citaFueraDelBloqueoSeAcepta() {
        prepararCitas(List.of());
        prepararBloqueo(LocalTime.of(10, 0), LocalTime.of(11, 0));

        assertDoesNotThrow(() -> disponibilidadCitaService.validar(doctor, FECHA, LocalTime.of(8, 0), null));
    }

    @Test
    void bloqueoDeVariosDiasCubreLaCita() {
        prepararHorario();
        when(bloqueoDoctorRepository.findByDoctorIdAndInicioLessThanAndFinGreaterThanOrderByInicioAsc(
                doctor.getId(), FECHA.plusDays(1).atStartOfDay(), FECHA.atStartOfDay()))
                .thenReturn(List.of(bloqueo(FECHA.minusDays(2).atStartOfDay(), FECHA.plusDays(2).atStartOfDay())));

        assertEstado(HttpStatus.CONFLICT, () -> disponibilidadCitaService.validar(doctor, FECHA, LocalTime.of(9, 0), null));
    }

    private void prepararBloqueo(LocalTime inicio, LocalTime fin) {
        when(bloqueoDoctorRepository.findByDoctorIdAndInicioLessThanAndFinGreaterThanOrderByInicioAsc(
                doctor.getId(), FECHA.plusDays(1).atStartOfDay(), FECHA.atStartOfDay()))
                .thenReturn(List.of(bloqueo(FECHA.atTime(inicio), FECHA.atTime(fin))));
    }

    private BloqueoDoctor bloqueo(LocalDateTime inicio, LocalDateTime fin) {
        return BloqueoDoctor.builder()
                .id(UUID.randomUUID())
                .doctor(doctor)
                .inicio(inicio)
                .fin(fin)
                .motivo("Congreso de ortodoncia")
                .build();
    }

    private void prepararHorario() {
        when(horarioRepository.findByDoctorIdAndDiaSemana(doctor.getId(), DIA)).thenReturn(List.of(Horario.builder()
                .id(UUID.randomUUID())
                .doctor(doctor)
                .diaSemana(DIA)
                .horaInicio(LocalTime.of(8, 0))
                .horaFin(LocalTime.of(12, 0))
                .build()));
    }

    private void prepararCitas(List<Cita> existentes) {
        prepararHorario();
        when(citaRepository.findByDoctorIdAndFechaAndEstadoNot(doctor.getId(), FECHA, EstadoCita.CANCELADA))
                .thenReturn(existentes);
    }

    private Cita cita(LocalTime hora) {
        return Cita.builder()
                .id(UUID.randomUUID())
                .doctor(doctor)
                .fecha(FECHA)
                .hora(hora)
                .estado(EstadoCita.AGENDADA)
                .build();
    }

    private void assertEstado(HttpStatus esperado, Runnable accion) {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, accion::run);
        assertEquals(esperado, exception.getStatusCode());
    }
}
