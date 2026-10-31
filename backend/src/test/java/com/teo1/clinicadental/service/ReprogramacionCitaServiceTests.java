package com.teo1.clinicadental.service;

import com.teo1.clinicadental.dto.CitaResponse;
import com.teo1.clinicadental.dto.ReprogramarCitaRequest;
import com.teo1.clinicadental.model.BloqueoDoctor;
import com.teo1.clinicadental.model.Cita;
import com.teo1.clinicadental.model.CitaReprogramacion;
import com.teo1.clinicadental.model.Cliente;
import com.teo1.clinicadental.model.DiaSemana;
import com.teo1.clinicadental.model.Doctor;
import com.teo1.clinicadental.model.EstadoCita;
import com.teo1.clinicadental.model.Horario;
import com.teo1.clinicadental.model.Usuario;
import com.teo1.clinicadental.repository.BloqueoDoctorRepository;
import com.teo1.clinicadental.repository.CitaReprogramacionRepository;
import com.teo1.clinicadental.repository.CitaRepository;
import com.teo1.clinicadental.repository.DoctorRepository;
import com.teo1.clinicadental.repository.HorarioRepository;
import com.teo1.clinicadental.repository.UsuarioRepository;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReprogramacionCitaServiceTests {

    private static final ZoneId ZONA = ZoneId.of("America/Guatemala");
    private static final LocalDate FECHA = LocalDate.of(2026, 10, 15);
    private static final DiaSemana DIA = DiaSemana.desde(FECHA.getDayOfWeek());
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 8, 8, 0);

    @Mock
    private CitaRepository citaRepository;

    @Mock
    private CitaReprogramacionRepository citaReprogramacionRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private HorarioRepository horarioRepository;

    @Mock
    private BloqueoDoctorRepository bloqueoDoctorRepository;

    @Mock
    private RecordatorioCitaService recordatorioCitaService;

    private ReprogramacionCitaService reprogramacionCitaService;
    private Doctor doctor;
    private Cliente cliente;
    private UUID usuarioClienteId;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(ZonedDateTime.of(AHORA, ZONA).toInstant(), ZONA);
        DisponibilidadCitaService disponibilidadCitaService = new DisponibilidadCitaService(
                citaRepository, horarioRepository, bloqueoDoctorRepository, clock, 30);
        reprogramacionCitaService = new ReprogramacionCitaService(
                citaRepository, citaReprogramacionRepository, doctorRepository, usuarioRepository,
                disponibilidadCitaService, recordatorioCitaService, new CitaMapper(), clock);

        usuarioClienteId = UUID.randomUUID();
        doctor = Doctor.builder()
                .id(UUID.randomUUID())
                .usuario(usuario(UUID.randomUUID(), "Carlos", "Perez"))
                .especialidad("Odontologia general")
                .build();
        cliente = Cliente.builder()
                .id(UUID.randomUUID())
                .usuario(usuario(usuarioClienteId, "Ana", "Lopez"))
                .dpi("1234567890101")
                .build();
    }

    @Test
    void secretariaYAdminReprogramanConMenosDe24Horas() {
        for (String rol : List.of("SECRETARIA", "ADMIN")) {
            Cita cita = prepararCita(LocalDate.of(2026, 10, 8), LocalTime.of(10, 0), EstadoCita.AGENDADA);
            prepararDisponibilidad(FECHA, List.of());
            prepararGuardado(cita);

            CitaResponse respuesta = reprogramacionCitaService.reprogramar(
                    cita.getId(), request(FECHA, LocalTime.of(10, 0), "Cambio de agenda"), autenticacion(UUID.randomUUID(), rol));

            assertEquals(FECHA, respuesta.getFecha());
            assertEquals(LocalTime.of(10, 0), respuesta.getHora());
        }
    }

    @Test
    void clienteReprogramaSuCitaConMasDe24Horas() {
        Cita cita = prepararCita(FECHA, LocalTime.of(9, 0), EstadoCita.AGENDADA);
        prepararDisponibilidad(FECHA, List.of(cita));
        prepararGuardado(cita);

        CitaResponse respuesta = reprogramacionCitaService.reprogramar(
                cita.getId(), request(FECHA, LocalTime.of(10, 0), "Cambio de turno"), autenticacion(usuarioClienteId, "CLIENTE"));

        assertEquals(LocalTime.of(10, 0), respuesta.getHora());
        assertEquals(EstadoCita.AGENDADA, respuesta.getEstado());
        assertEquals(cliente.getId(), respuesta.getIdCliente());
        assertEquals(doctor.getId(), respuesta.getIdDoctor());
        verify(recordatorioCitaService).enviarAlAgendar(cita);
    }

    @Test
    void clienteReprogramaConExactamente24Horas() {
        Cita cita = prepararCita(LocalDate.of(2026, 10, 9), LocalTime.of(8, 0), EstadoCita.AGENDADA);
        prepararDisponibilidad(FECHA, List.of());
        prepararGuardado(cita);

        CitaResponse respuesta = reprogramacionCitaService.reprogramar(
                cita.getId(), request(FECHA, LocalTime.of(10, 0), "Cambio de turno"), autenticacion(usuarioClienteId, "CLIENTE"));

        assertEquals(FECHA, respuesta.getFecha());
    }

    @Test
    void clienteReprogramaElMismoDiaSeAcepta() {
        LocalDate hoy = AHORA.toLocalDate();
        Cita cita = prepararCita(hoy, LocalTime.of(9, 0), EstadoCita.AGENDADA);
        prepararDisponibilidad(hoy, List.of());
        prepararGuardado(cita);

        CitaResponse respuesta = reprogramacionCitaService.reprogramar(
                cita.getId(), request(hoy, LocalTime.of(10, 0), "Cambio de turno"), autenticacion(usuarioClienteId, "CLIENTE"));

        assertEquals(hoy, respuesta.getFecha());
        assertEquals(LocalTime.of(10, 0), respuesta.getHora());
    }

    @Test
    void clienteNoPuedeReprogramarCitaAjena() {
        Cita cita = prepararCita(FECHA, LocalTime.of(9, 0), EstadoCita.AGENDADA);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> reprogramacionCitaService.reprogramar(
                cita.getId(), request(FECHA, LocalTime.of(10, 0), "Cambio"), autenticacion(UUID.randomUUID(), "CLIENTE")));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("No tienes permiso para reprogramar esta cita", exception.getReason());
        assertSinCambios(cita, FECHA, LocalTime.of(9, 0));
    }

    @Test
    void doctorNoPuedeReprogramarNiSuPropiaCita() {
        Cita cita = prepararCita(FECHA, LocalTime.of(9, 0), EstadoCita.AGENDADA);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> reprogramacionCitaService.reprogramar(
                cita.getId(), request(FECHA, LocalTime.of(10, 0), "Cambio"), autenticacion(doctor.getUsuario().getId(), "DOCTOR")));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("Los doctores no pueden reprogramar citas", exception.getReason());
        assertSinCambios(cita, FECHA, LocalTime.of(9, 0));
    }

    @Test
    void citaQueNoEstaAgendadaDevuelveConflicto() {
        for (EstadoCita estado : List.of(EstadoCita.CANCELADA, EstadoCita.ATENDIDA, EstadoCita.NO_ASISTIO)) {
            Cita cita = prepararCita(FECHA, LocalTime.of(9, 0), estado);

            ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> reprogramacionCitaService.reprogramar(
                    cita.getId(), request(FECHA, LocalTime.of(10, 0), "Cambio"), autenticacion(UUID.randomUUID(), "ADMIN")));

            assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
            assertEquals("Solo se puede reprogramar una cita AGENDADA y esta cita esta " + estado, exception.getReason());
        }
        verify(citaRepository, never()).saveAndFlush(any());
    }

    @Test
    void citaInexistenteDevuelveNoEncontrado() {
        UUID id = UUID.randomUUID();
        when(citaRepository.findConRelacionesById(id)).thenReturn(Optional.empty());

        assertEstado(HttpStatus.NOT_FOUND, () -> reprogramacionCitaService.reprogramar(
                id, request(FECHA, LocalTime.of(10, 0), "Cambio"), autenticacion(UUID.randomUUID(), "ADMIN")));
    }

    @Test
    void motivoVacioEsInvalido() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            for (String motivo : Arrays.asList(null, "", "   ", "x".repeat(201))) {
                assertTrue(factory.getValidator().validate(request(FECHA, LocalTime.of(10, 0), motivo)).stream()
                        .anyMatch(error -> "motivo".equals(error.getPropertyPath().toString())));
            }
            assertEquals(2, factory.getValidator().validate(request(null, null, "Cambio")).size());
            assertTrue(factory.getValidator().validate(request(FECHA, LocalTime.of(10, 0), "x".repeat(200))).isEmpty());
        }
    }

    @Test
    void nuevaFechaYHoraIgualesALasActualesDevuelveSolicitudInvalida() {
        Cita cita = prepararCita(FECHA, LocalTime.of(9, 0), EstadoCita.AGENDADA);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> reprogramacionCitaService.reprogramar(
                cita.getId(), request(FECHA, LocalTime.of(9, 0), "Cambio"), autenticacion(usuarioClienteId, "CLIENTE")));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("La nueva fecha y hora son iguales a las actuales", exception.getReason());
    }

    @Test
    void moverDentroDeSuPropioHorarioNoChocaConsigoMisma() {
        Cita cita = prepararCita(FECHA, LocalTime.of(9, 0), EstadoCita.AGENDADA);
        prepararDisponibilidad(FECHA, List.of(cita));
        prepararGuardado(cita);

        CitaResponse respuesta = reprogramacionCitaService.reprogramar(
                cita.getId(), request(FECHA, LocalTime.of(9, 15), "Llega tarde"), autenticacion(usuarioClienteId, "CLIENTE"));

        assertEquals(LocalTime.of(9, 15), respuesta.getHora());
    }

    @Test
    void choqueConOtraCitaDevuelveConflicto() {
        Cita cita = prepararCita(FECHA, LocalTime.of(9, 0), EstadoCita.AGENDADA);
        Cita otra = Cita.builder().id(UUID.randomUUID()).doctor(doctor).fecha(FECHA).hora(LocalTime.of(10, 0))
                .estado(EstadoCita.AGENDADA).build();
        prepararDisponibilidad(FECHA, List.of(cita, otra));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> reprogramacionCitaService.reprogramar(
                cita.getId(), request(FECHA, LocalTime.of(10, 15), "Cambio"), autenticacion(usuarioClienteId, "CLIENTE")));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals(DisponibilidadCitaService.MENSAJE_SOLAPE, exception.getReason());
        assertSinCambios(cita, FECHA, LocalTime.of(9, 0));
    }

    @Test
    void fueraDelHorarioDevuelveConflicto() {
        Cita cita = prepararCita(FECHA, LocalTime.of(9, 0), EstadoCita.AGENDADA);
        prepararDoctorBloqueado();
        prepararHorario(FECHA);

        assertEstado(HttpStatus.CONFLICT, () -> reprogramacionCitaService.reprogramar(
                cita.getId(), request(FECHA, LocalTime.of(11, 45), "Cambio"), autenticacion(usuarioClienteId, "CLIENTE")));
        assertSinCambios(cita, FECHA, LocalTime.of(9, 0));
    }

    @Test
    void dentroDeUnBloqueoDevuelveConflicto() {
        Cita cita = prepararCita(FECHA, LocalTime.of(9, 0), EstadoCita.AGENDADA);
        prepararDoctorBloqueado();
        prepararHorario(FECHA);
        when(bloqueoDoctorRepository.findByDoctorIdAndInicioLessThanAndFinGreaterThanOrderByInicioAsc(
                doctor.getId(), FECHA.plusDays(1).atStartOfDay(), FECHA.atStartOfDay()))
                .thenReturn(List.of(BloqueoDoctor.builder().id(UUID.randomUUID()).doctor(doctor)
                        .inicio(FECHA.atTime(10, 0)).fin(FECHA.atTime(11, 0)).motivo("Congreso").build()));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> reprogramacionCitaService.reprogramar(
                cita.getId(), request(FECHA, LocalTime.of(10, 0), "Cambio"), autenticacion(usuarioClienteId, "CLIENTE")));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("El doctor no atiende en ese horario: Congreso", exception.getReason());
        assertSinCambios(cita, FECHA, LocalTime.of(9, 0));
    }

    @Test
    void fechaPasadaDevuelveSolicitudInvalida() {
        Cita cita = prepararCita(FECHA, LocalTime.of(9, 0), EstadoCita.AGENDADA);
        prepararDoctorBloqueado();

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> reprogramacionCitaService.reprogramar(
                cita.getId(), request(LocalDate.of(2026, 10, 7), LocalTime.of(9, 0), "Cambio"), autenticacion(UUID.randomUUID(), "ADMIN")));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("La fecha y hora de la cita ya pasaron", exception.getReason());
        assertSinCambios(cita, FECHA, LocalTime.of(9, 0));
    }

    @Test
    void registraLaReprogramacionYReiniciaElRecordatorio() {
        Cita cita = prepararCita(FECHA, LocalTime.of(9, 0), EstadoCita.AGENDADA);
        cita.setRecordatorioEnviado(true);
        prepararDisponibilidad(FECHA.plusDays(7), List.of());
        prepararGuardado(cita);
        Usuario secretaria = usuario(UUID.randomUUID(), "Rosa", "Mendez");
        when(usuarioRepository.getReferenceById(secretaria.getId())).thenReturn(secretaria);

        CitaResponse respuesta = reprogramacionCitaService.reprogramar(cita.getId(),
                request(FECHA.plusDays(7), LocalTime.of(11, 0), "  Cambio de turno  "), autenticacion(secretaria.getId(), "SECRETARIA"));

        ArgumentCaptor<CitaReprogramacion> captor = ArgumentCaptor.forClass(CitaReprogramacion.class);
        verify(citaReprogramacionRepository).save(captor.capture());
        CitaReprogramacion registro = captor.getValue();
        assertEquals(cita, registro.getCita());
        assertEquals(FECHA, registro.getFechaAnterior());
        assertEquals(LocalTime.of(9, 0), registro.getHoraAnterior());
        assertEquals(FECHA.plusDays(7), registro.getFechaNueva());
        assertEquals(LocalTime.of(11, 0), registro.getHoraNueva());
        assertEquals("Cambio de turno", registro.getMotivo());
        assertEquals(secretaria, registro.getReprogramadoPor());
        assertEquals(AHORA, registro.getFechaCambio());

        assertFalse(cita.isRecordatorioEnviado());
        assertEquals(EstadoCita.AGENDADA, cita.getEstado());
        assertNull(cita.getMotivoEstado());
        assertNull(respuesta.getMotivo());
    }

    @Test
    void violacionDelIndiceUnicoDevuelveConflictoSinRegistrar() {
        Cita cita = prepararCita(FECHA, LocalTime.of(9, 0), EstadoCita.AGENDADA);
        prepararDisponibilidad(FECHA, List.of(cita));
        when(citaRepository.saveAndFlush(cita)).thenThrow(new DataIntegrityViolationException(
                "could not execute statement",
                new RuntimeException("duplicate key value violates unique constraint \"uq_cita_doctor_slot\"")));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> reprogramacionCitaService.reprogramar(
                cita.getId(), request(FECHA, LocalTime.of(10, 0), "Cambio"), autenticacion(usuarioClienteId, "CLIENTE")));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals(DisponibilidadCitaService.MENSAJE_SOLAPE, exception.getReason());
        verify(citaReprogramacionRepository, never()).save(any());
    }

    private Cita prepararCita(LocalDate fecha, LocalTime hora, EstadoCita estado) {
        Cita cita = Cita.builder()
                .id(UUID.randomUUID())
                .cliente(cliente)
                .doctor(doctor)
                .fecha(fecha)
                .hora(hora)
                .estado(estado)
                .build();
        when(citaRepository.findConRelacionesById(cita.getId())).thenReturn(Optional.of(cita));
        return cita;
    }

    private void prepararDoctorBloqueado() {
        when(doctorRepository.findByIdForUpdate(doctor.getId())).thenReturn(Optional.of(doctor));
    }

    private void prepararHorario(LocalDate fecha) {
        when(horarioRepository.findByDoctorIdAndDiaSemana(doctor.getId(), DiaSemana.desde(fecha.getDayOfWeek())))
                .thenReturn(List.of(Horario.builder()
                        .id(UUID.randomUUID())
                        .doctor(doctor)
                        .diaSemana(DIA)
                        .horaInicio(LocalTime.of(8, 0))
                        .horaFin(LocalTime.of(12, 0))
                        .build()));
    }

    private void prepararDisponibilidad(LocalDate fecha, List<Cita> existentes) {
        prepararDoctorBloqueado();
        prepararHorario(fecha);
        when(citaRepository.findByDoctorIdAndFechaAndEstadoNot(doctor.getId(), fecha, EstadoCita.CANCELADA))
                .thenReturn(existentes);
    }

    private void prepararGuardado(Cita cita) {
        when(citaRepository.saveAndFlush(cita)).thenReturn(cita);
    }

    private void assertSinCambios(Cita cita, LocalDate fecha, LocalTime hora) {
        assertEquals(fecha, cita.getFecha());
        assertEquals(hora, cita.getHora());
        verify(citaRepository, never()).saveAndFlush(any());
        verify(citaReprogramacionRepository, never()).save(any());
    }

    private ReprogramarCitaRequest request(LocalDate fecha, LocalTime hora, String motivo) {
        ReprogramarCitaRequest request = new ReprogramarCitaRequest();
        request.setFecha(fecha);
        request.setHora(hora);
        request.setMotivo(motivo);
        return request;
    }

    private Authentication autenticacion(UUID usuarioId, String rol) {
        return UsernamePasswordAuthenticationToken.authenticated(
                usuarioId.toString(), null, List.of(new SimpleGrantedAuthority("ROLE_" + rol)));
    }

    private Usuario usuario(UUID id, String nombre, String apellido) {
        return Usuario.builder()
                .id(id)
                .nombre(nombre)
                .apellido(apellido)
                .email(nombre.toLowerCase() + "@clinica.com")
                .passwordHash("hash")
                .build();
    }

    private void assertEstado(HttpStatus esperado, Runnable accion) {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, accion::run);
        assertEquals(esperado, exception.getStatusCode());
    }
}
