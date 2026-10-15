package com.teo1.clinicadental.service;

import com.teo1.clinicadental.dto.BloqueoDoctorRequest;
import com.teo1.clinicadental.dto.BloqueoDoctorResponse;
import com.teo1.clinicadental.model.BloqueoDoctor;
import com.teo1.clinicadental.model.Cita;
import com.teo1.clinicadental.model.Doctor;
import com.teo1.clinicadental.model.EstadoCita;
import com.teo1.clinicadental.model.Usuario;
import com.teo1.clinicadental.repository.BloqueoDoctorRepository;
import com.teo1.clinicadental.repository.CitaRepository;
import com.teo1.clinicadental.repository.DoctorRepository;
import com.teo1.clinicadental.repository.UsuarioRepository;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
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
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BloqueoDoctorServiceTests {

    private static final ZoneId ZONA = ZoneId.of("America/Guatemala");
    private static final LocalDate FECHA = LocalDate.of(2026, 10, 15);

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @Mock
    private BloqueoDoctorRepository bloqueoDoctorRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private CitaRepository citaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    private BloqueoDoctorService bloqueoDoctorService;
    private Doctor doctor;
    private UUID usuarioSecretariaId;

    @BeforeAll
    static void crearValidador() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void cerrarValidador() {
        validatorFactory.close();
    }

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(ZonedDateTime.of(2026, 10, 8, 8, 0, 0, 0, ZONA).toInstant(), ZONA);
        bloqueoDoctorService = new BloqueoDoctorService(
                bloqueoDoctorRepository, doctorRepository, citaRepository, usuarioRepository, clock, 30);
        usuarioSecretariaId = UUID.randomUUID();
        doctor = Doctor.builder()
                .id(UUID.randomUUID())
                .usuario(Usuario.builder().id(UUID.randomUUID()).nombre("Carlos").apellido("Perez").build())
                .especialidad("Ortodoncia")
                .build();
    }

    @Test
    void crearBloqueoValidoLoGuardaConQuienLoCreo() {
        prepararDoctor();
        prepararCitas(List.of());
        Usuario secretaria = Usuario.builder().id(usuarioSecretariaId).build();
        when(usuarioRepository.getReferenceById(usuarioSecretariaId)).thenReturn(secretaria);
        when(bloqueoDoctorRepository.save(any(BloqueoDoctor.class))).thenAnswer(invocacion -> {
            BloqueoDoctor bloqueo = invocacion.getArgument(0);
            bloqueo.setId(UUID.randomUUID());
            return bloqueo;
        });

        BloqueoDoctorResponse respuesta = bloqueoDoctorService.crearBloqueo(
                doctor.getId(), request(10, 0, 11, 0, "  Congreso de ortodoncia  "), autenticacion(usuarioSecretariaId, "SECRETARIA"));

        ArgumentCaptor<BloqueoDoctor> captor = ArgumentCaptor.forClass(BloqueoDoctor.class);
        verify(bloqueoDoctorRepository).save(captor.capture());
        assertEquals(doctor, captor.getValue().getDoctor());
        assertEquals(secretaria, captor.getValue().getCreadoPor());
        assertEquals(LocalDateTime.of(2026, 10, 8, 8, 0), captor.getValue().getFechaCreacion());
        assertEquals("Congreso de ortodoncia", respuesta.getMotivo());
        assertEquals(FECHA.atTime(10, 0), respuesta.getInicio());
        assertEquals(FECHA.atTime(11, 0), respuesta.getFin());
        assertEquals(usuarioSecretariaId, respuesta.getIdCreadoPor());
        assertEquals(doctor.getId(), respuesta.getIdDoctor());
    }

    @Test
    void inicioIgualOPosteriorAlFinDevuelveSolicitudInvalida() {
        for (BloqueoDoctorRequest request : List.of(request(10, 0, 10, 0, "Congreso"), request(11, 0, 10, 0, "Congreso"))) {
            ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                    () -> bloqueoDoctorService.crearBloqueo(doctor.getId(), request, autenticacion(usuarioSecretariaId, "ADMIN")));
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            assertEquals("El inicio del bloqueo debe ser anterior al fin", exception.getReason());
        }
        verifyNoInteractions(doctorRepository, bloqueoDoctorRepository);
    }

    @Test
    void sinMotivoOSinFechasLaPeticionEsInvalida() {
        for (String motivo : Arrays.asList(null, "", "   ")) {
            Set<ConstraintViolation<BloqueoDoctorRequest>> errores = validator.validate(request(10, 0, 11, 0, motivo));
            assertTrue(errores.stream().anyMatch(error -> "motivo".equals(error.getPropertyPath().toString())));
        }

        BloqueoDoctorRequest sinFechas = new BloqueoDoctorRequest();
        sinFechas.setMotivo("Congreso");
        Set<ConstraintViolation<BloqueoDoctorRequest>> errores = validator.validate(sinFechas);
        assertEquals(2, errores.size());

        assertTrue(validator.validate(request(10, 0, 11, 0, "x".repeat(201))).stream()
                .anyMatch(error -> "motivo".equals(error.getPropertyPath().toString())));
        assertTrue(validator.validate(request(10, 0, 11, 0, "x".repeat(200))).isEmpty());
    }

    @Test
    void doctorInexistenteDevuelveNoEncontrado() {
        when(doctorRepository.findByIdForUpdate(doctor.getId())).thenReturn(Optional.empty());

        assertEstado(HttpStatus.NOT_FOUND, () -> bloqueoDoctorService.crearBloqueo(
                doctor.getId(), request(10, 0, 11, 0, "Congreso"), autenticacion(usuarioSecretariaId, "ADMIN")));
        verify(bloqueoDoctorRepository, never()).save(any());
    }

    @Test
    void citasAgendadasEnElPeriodoDevuelvenConflictoConLaCantidad() {
        prepararDoctor();
        prepararCitas(List.of(
                cita(LocalTime.of(9, 30)),
                cita(LocalTime.of(9, 45)),
                cita(LocalTime.of(10, 30)),
                cita(LocalTime.of(11, 0))));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> bloqueoDoctorService.crearBloqueo(
                doctor.getId(), request(10, 0, 11, 0, "Congreso"), autenticacion(usuarioSecretariaId, "ADMIN")));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        assertEquals("El doctor tiene 2 citas agendadas en ese periodo; primero deben reprogramarse o cancelarse",
                exception.getReason());
        verify(bloqueoDoctorRepository, never()).save(any());
    }

    @Test
    void unaSolaCitaAgendadaUsaElSingular() {
        prepararDoctor();
        prepararCitas(List.of(cita(LocalTime.of(10, 0))));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> bloqueoDoctorService.crearBloqueo(
                doctor.getId(), request(10, 0, 11, 0, "Congreso"), autenticacion(usuarioSecretariaId, "ADMIN")));

        assertEquals("El doctor tiene 1 cita agendada en ese periodo; primero deben reprogramarse o cancelarse",
                exception.getReason());
    }

    @Test
    void citaCanceladaEnElPeriodoNoImpideElBloqueo() {
        prepararDoctor();
        prepararCitas(List.of());
        when(bloqueoDoctorRepository.save(any(BloqueoDoctor.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        bloqueoDoctorService.crearBloqueo(doctor.getId(), request(10, 0, 11, 0, "Congreso"), autenticacion(usuarioSecretariaId, "ADMIN"));

        verify(citaRepository).findByDoctorIdAndEstadoAndFechaBetweenOrderByFechaAscHoraAsc(
                doctor.getId(), EstadoCita.AGENDADA, FECHA, FECHA);
        verify(bloqueoDoctorRepository).save(any(BloqueoDoctor.class));
    }

    @Test
    void borrarBloqueoDelDoctorLoElimina() {
        BloqueoDoctor bloqueo = bloqueo();
        when(bloqueoDoctorRepository.findByIdAndDoctorId(bloqueo.getId(), doctor.getId())).thenReturn(Optional.of(bloqueo));

        bloqueoDoctorService.eliminarBloqueo(doctor.getId(), bloqueo.getId());

        verify(bloqueoDoctorRepository).delete(bloqueo);
    }

    @Test
    void borrarBloqueoInexistenteODeOtroDoctorDevuelveNoEncontrado() {
        UUID idBloqueo = UUID.randomUUID();
        when(bloqueoDoctorRepository.findByIdAndDoctorId(idBloqueo, doctor.getId())).thenReturn(Optional.empty());

        assertEstado(HttpStatus.NOT_FOUND, () -> bloqueoDoctorService.eliminarBloqueo(doctor.getId(), idBloqueo));
        verify(bloqueoDoctorRepository, never()).delete(any());
    }

    @Test
    void doctorListaSusPropiosBloqueos() {
        when(doctorRepository.findById(doctor.getId())).thenReturn(Optional.of(doctor));
        when(bloqueoDoctorRepository.findByDoctorIdOrderByInicioAsc(doctor.getId())).thenReturn(List.of(bloqueo()));

        List<BloqueoDoctorResponse> bloqueos = bloqueoDoctorService.listarBloqueos(
                doctor.getId(), autenticacion(doctor.getUsuario().getId(), "DOCTOR"));

        assertEquals(1, bloqueos.size());
    }

    @Test
    void doctorNoPuedeListarBloqueosDeOtroDoctor() {
        when(doctorRepository.findById(doctor.getId())).thenReturn(Optional.of(doctor));

        assertEstado(HttpStatus.FORBIDDEN, () -> bloqueoDoctorService.listarBloqueos(
                doctor.getId(), autenticacion(UUID.randomUUID(), "DOCTOR")));
        verifyNoInteractions(bloqueoDoctorRepository);
    }

    @Test
    void clienteNoPuedeListarBloqueos() {
        assertEstado(HttpStatus.FORBIDDEN, () -> bloqueoDoctorService.listarBloqueos(
                doctor.getId(), autenticacion(UUID.randomUUID(), "CLIENTE")));
        verifyNoInteractions(doctorRepository, bloqueoDoctorRepository);
    }

    @Test
    void listarBloqueosDeDoctorInexistenteDevuelveNoEncontrado() {
        when(doctorRepository.findById(doctor.getId())).thenReturn(Optional.empty());

        assertEstado(HttpStatus.NOT_FOUND, () -> bloqueoDoctorService.listarBloqueos(
                doctor.getId(), autenticacion(UUID.randomUUID(), "SECRETARIA")));
    }

    private void prepararDoctor() {
        when(doctorRepository.findByIdForUpdate(doctor.getId())).thenReturn(Optional.of(doctor));
    }

    private void prepararCitas(List<Cita> citas) {
        when(citaRepository.findByDoctorIdAndEstadoAndFechaBetweenOrderByFechaAscHoraAsc(
                doctor.getId(), EstadoCita.AGENDADA, FECHA, FECHA)).thenReturn(citas);
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

    private BloqueoDoctor bloqueo() {
        return BloqueoDoctor.builder()
                .id(UUID.randomUUID())
                .doctor(doctor)
                .inicio(FECHA.atTime(10, 0))
                .fin(FECHA.atTime(11, 0))
                .motivo("Congreso")
                .fechaCreacion(LocalDateTime.of(2026, 10, 8, 8, 0))
                .build();
    }

    private BloqueoDoctorRequest request(int horaInicio, int minutoInicio, int horaFin, int minutoFin, String motivo) {
        BloqueoDoctorRequest request = new BloqueoDoctorRequest();
        request.setInicio(FECHA.atTime(horaInicio, minutoInicio));
        request.setFin(FECHA.atTime(horaFin, minutoFin));
        request.setMotivo(motivo);
        return request;
    }

    private Authentication autenticacion(UUID usuarioId, String rol) {
        return UsernamePasswordAuthenticationToken.authenticated(
                usuarioId.toString(), null, List.of(new SimpleGrantedAuthority("ROLE_" + rol)));
    }

    private void assertEstado(HttpStatus esperado, Runnable accion) {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, accion::run);
        assertEquals(esperado, exception.getStatusCode());
    }
}
