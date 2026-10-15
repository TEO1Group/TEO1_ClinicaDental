package com.teo1.clinicadental.service;

import com.teo1.clinicadental.dto.CitaTratamientoRequest;
import com.teo1.clinicadental.dto.CitaTratamientoResponse;
import com.teo1.clinicadental.dto.TratamientoResponse;
import com.teo1.clinicadental.dto.TratamientosPacienteResponse;
import com.teo1.clinicadental.model.Cita;
import com.teo1.clinicadental.model.CitaTratamiento;
import com.teo1.clinicadental.model.Cliente;
import com.teo1.clinicadental.model.Doctor;
import com.teo1.clinicadental.model.EstadoCita;
import com.teo1.clinicadental.model.Tratamiento;
import com.teo1.clinicadental.model.Usuario;
import com.teo1.clinicadental.repository.CitaRepository;
import com.teo1.clinicadental.repository.CitaTratamientoRepository;
import com.teo1.clinicadental.repository.ClienteRepository;
import com.teo1.clinicadental.repository.TratamientoRepository;
import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TratamientoServiceTests {

    private static final ZoneId ZONA = ZoneId.of("America/Guatemala");
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 8, 10, 0);

    @Mock
    private TratamientoRepository tratamientoRepository;

    @Mock
    private CitaTratamientoRepository citaTratamientoRepository;

    @Mock
    private CitaRepository citaRepository;

    @Mock
    private ClienteRepository clienteRepository;

    private TratamientoService tratamientoService;
    private Doctor doctor;
    private Cliente cliente;
    private Tratamiento limpieza;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(ZonedDateTime.of(AHORA, ZONA).toInstant(), ZONA);
        tratamientoService = new TratamientoService(
                tratamientoRepository, citaTratamientoRepository, citaRepository, clienteRepository, clock);

        doctor = Doctor.builder()
                .id(UUID.randomUUID())
                .usuario(Usuario.builder().id(UUID.randomUUID()).nombre("Carlos").apellido("Perez").build())
                .especialidad("Odontologia general")
                .build();
        cliente = Cliente.builder()
                .id(UUID.randomUUID())
                .usuario(Usuario.builder().id(UUID.randomUUID()).nombre("Ana").apellido("Lopez").build())
                .dpi("1234567890101")
                .build();
        limpieza = Tratamiento.builder()
                .id(UUID.randomUUID())
                .nombre("Limpieza dental")
                .descripcion("Profilaxis")
                .duracionMinutos(45)
                .costo(new BigDecimal("300.00"))
                .build();
    }

    @Test
    void catalogoDevuelveTodosLosCampos() {
        when(tratamientoRepository.findAllByOrderByNombreAsc()).thenReturn(List.of(limpieza));

        List<TratamientoResponse> catalogo = tratamientoService.listarCatalogo();

        assertEquals(1, catalogo.size());
        assertEquals(limpieza.getId(), catalogo.get(0).getIdTratamiento());
        assertEquals("Limpieza dental", catalogo.get(0).getNombre());
        assertEquals("Profilaxis", catalogo.get(0).getDescripcion());
        assertEquals(45, catalogo.get(0).getDuracionMinutos());
        assertEquals(new BigDecimal("300.00"), catalogo.get(0).getCosto());
    }

    @Test
    void registroSinCostoUsaElDelCatalogo() {
        Cita cita = prepararCita(EstadoCita.ATENDIDA);
        prepararTratamiento();
        prepararGuardado();

        CitaTratamientoResponse respuesta = tratamientoService.registrar(
                cita.getId(), request(null, "  Sin complicaciones  "), autenticacionDoctor());

        ArgumentCaptor<CitaTratamiento> captor = ArgumentCaptor.forClass(CitaTratamiento.class);
        verify(citaTratamientoRepository).save(captor.capture());
        assertEquals(new BigDecimal("300.00"), captor.getValue().getCosto());
        assertEquals(AHORA, captor.getValue().getFechaRegistro());
        assertEquals(cita, captor.getValue().getCita());
        assertEquals("Sin complicaciones", respuesta.getObservaciones());
        assertEquals(new BigDecimal("300.00"), respuesta.getCosto());
        assertEquals(cita.getId(), respuesta.getIdCita());
        assertEquals(cita.getFecha(), respuesta.getFechaCita());
        assertEquals(cita.getHora(), respuesta.getHoraCita());
        assertEquals("Carlos", respuesta.getNombreDoctor());
        assertEquals("Perez", respuesta.getApellidoDoctor());
        assertEquals(limpieza.getId(), respuesta.getIdTratamiento());
        assertEquals("Limpieza dental", respuesta.getNombreTratamiento());
    }

    @Test
    void registroConCostoAjustadoGuardaEseCosto() {
        Cita cita = prepararCita(EstadoCita.ATENDIDA);
        prepararTratamiento();
        prepararGuardado();

        CitaTratamientoResponse respuesta = tratamientoService.registrar(
                cita.getId(), request(new BigDecimal("250.50"), "   "), autenticacionDoctor());

        assertEquals(new BigDecimal("250.50"), respuesta.getCosto());
        assertNull(respuesta.getObservaciones());
    }

    @Test
    void registroConCostoCeroSeAcepta() {
        Cita cita = prepararCita(EstadoCita.ATENDIDA);
        prepararTratamiento();
        prepararGuardado();

        assertEquals(BigDecimal.ZERO, tratamientoService.registrar(
                cita.getId(), request(BigDecimal.ZERO, null), autenticacionDoctor()).getCosto());
    }

    @Test
    void costoNegativoDevuelveSolicitudInvalida() {
        Cita cita = prepararCita(EstadoCita.ATENDIDA);
        prepararTratamiento();

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> tratamientoService.registrar(
                cita.getId(), request(new BigDecimal("-0.01"), null), autenticacionDoctor()));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("El costo no puede ser negativo", exception.getReason());
        verify(citaTratamientoRepository, never()).save(any());
    }

    @Test
    void validacionDeLaPeticion() {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            assertTrue(factory.getValidator().validate(request(new BigDecimal("-1"), null)).stream()
                    .anyMatch(error -> "costo".equals(error.getPropertyPath().toString())));
            assertTrue(factory.getValidator().validate(request(new BigDecimal("1.234"), null)).stream()
                    .anyMatch(error -> "costo".equals(error.getPropertyPath().toString())));
            assertTrue(factory.getValidator().validate(request(null, "x".repeat(1001))).stream()
                    .anyMatch(error -> "observaciones".equals(error.getPropertyPath().toString())));

            CitaTratamientoRequest sinTratamiento = new CitaTratamientoRequest();
            assertEquals(1, factory.getValidator().validate(sinTratamiento).size());
            assertTrue(factory.getValidator().validate(request(null, null)).isEmpty());
        }
    }

    @Test
    void citaNoAtendidaDevuelveConflicto() {
        for (EstadoCita estado : List.of(EstadoCita.AGENDADA, EstadoCita.CANCELADA, EstadoCita.NO_ASISTIO)) {
            Cita cita = prepararCita(estado);

            ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> tratamientoService.registrar(
                    cita.getId(), request(null, null), autenticacionDoctor()));

            assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
            assertEquals("Solo se pueden registrar tratamientos en una cita ATENDIDA y esta cita esta " + estado,
                    exception.getReason());
        }
        verifyNoInteractions(tratamientoRepository, citaTratamientoRepository);
    }

    @Test
    void otroDoctorAdminSecretariaYClienteNoPuedenRegistrar() {
        Cita cita = prepararCita(EstadoCita.ATENDIDA);

        for (Authentication autenticacion : List.of(
                autenticacion(UUID.randomUUID(), "DOCTOR"),
                autenticacion(UUID.randomUUID(), "ADMIN"),
                autenticacion(UUID.randomUUID(), "SECRETARIA"),
                autenticacion(cliente.getUsuario().getId(), "CLIENTE"))) {
            ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                    () -> tratamientoService.registrar(cita.getId(), request(null, null), autenticacion));
            assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
            assertEquals("Solo el doctor de la cita puede registrar tratamientos", exception.getReason());
        }
        verifyNoInteractions(tratamientoRepository, citaTratamientoRepository);
    }

    @Test
    void citaInexistenteDevuelveNoEncontrado() {
        UUID idCita = UUID.randomUUID();
        when(citaRepository.findConRelacionesById(idCita)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> tratamientoService.registrar(idCita, request(null, null), autenticacionDoctor()));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Cita no encontrada", exception.getReason());
    }

    @Test
    void tratamientoInexistenteDevuelveNoEncontrado() {
        Cita cita = prepararCita(EstadoCita.ATENDIDA);
        when(tratamientoRepository.findById(limpieza.getId())).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> tratamientoService.registrar(cita.getId(), request(null, null), autenticacionDoctor()));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Tratamiento no encontrado", exception.getReason());
        verify(citaTratamientoRepository, never()).save(any());
    }

    @Test
    void listarPorCitaSegunElRol() {
        Cita cita = prepararCita(EstadoCita.ATENDIDA);
        when(citaTratamientoRepository.findByCitaIdOrderByFechaRegistroAsc(cita.getId()))
                .thenReturn(List.of(registro(cita, "300.00")));

        assertEquals(1, tratamientoService.listarPorCita(cita.getId(), autenticacionDoctor()).size());
        assertEquals(1, tratamientoService.listarPorCita(cita.getId(), autenticacion(UUID.randomUUID(), "ADMIN")).size());
        assertEquals(1, tratamientoService.listarPorCita(cita.getId(), autenticacion(UUID.randomUUID(), "SECRETARIA")).size());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> tratamientoService.listarPorCita(cita.getId(), autenticacion(UUID.randomUUID(), "DOCTOR")));
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("Solo puedes ver los tratamientos de tus citas", exception.getReason());
    }

    @Test
    void totalDelPacienteSumaLosCostos() {
        Cita primera = prepararCitaSinBuscar(LocalDate.of(2026, 10, 1));
        Cita segunda = prepararCitaSinBuscar(LocalDate.of(2026, 10, 5));
        when(clienteRepository.existsById(cliente.getId())).thenReturn(true);
        when(citaTratamientoRepository.findByCitaClienteIdOrderByCitaFechaDescCitaHoraDescFechaRegistroAsc(cliente.getId()))
                .thenReturn(List.of(registro(segunda, "350.00"), registro(segunda, "350.00"), registro(primera, "150.50")));

        TratamientosPacienteResponse respuesta = tratamientoService.listarPorPaciente(cliente.getId());

        assertEquals(new BigDecimal("850.50"), respuesta.getTotal());
        assertEquals(3, respuesta.getTratamientos().size());
        assertEquals(segunda.getId(), respuesta.getTratamientos().get(0).getIdCita());
    }

    @Test
    void pacienteSinTratamientosTieneTotalCero() {
        when(clienteRepository.existsById(cliente.getId())).thenReturn(true);
        when(citaTratamientoRepository.findByCitaClienteIdOrderByCitaFechaDescCitaHoraDescFechaRegistroAsc(cliente.getId()))
                .thenReturn(List.of());

        TratamientosPacienteResponse respuesta = tratamientoService.listarPorPaciente(cliente.getId());

        assertEquals(0, BigDecimal.ZERO.compareTo(respuesta.getTotal()));
        assertTrue(respuesta.getTratamientos().isEmpty());
    }

    @Test
    void pacienteInexistenteDevuelveNoEncontrado() {
        UUID idCliente = UUID.randomUUID();
        when(clienteRepository.existsById(idCliente)).thenReturn(false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> tratamientoService.listarPorPaciente(idCliente));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        assertEquals("Paciente no encontrado", exception.getReason());
        verifyNoInteractions(citaTratamientoRepository);
    }

    private Cita prepararCita(EstadoCita estado) {
        Cita cita = prepararCitaSinBuscar(LocalDate.of(2026, 10, 8));
        cita.setEstado(estado);
        when(citaRepository.findConRelacionesById(cita.getId())).thenReturn(Optional.of(cita));
        return cita;
    }

    private Cita prepararCitaSinBuscar(LocalDate fecha) {
        return Cita.builder()
                .id(UUID.randomUUID())
                .cliente(cliente)
                .doctor(doctor)
                .fecha(fecha)
                .hora(LocalTime.of(9, 0))
                .estado(EstadoCita.ATENDIDA)
                .build();
    }

    private void prepararTratamiento() {
        when(tratamientoRepository.findById(limpieza.getId())).thenReturn(Optional.of(limpieza));
    }

    private void prepararGuardado() {
        when(citaTratamientoRepository.save(any(CitaTratamiento.class))).thenAnswer(invocacion -> {
            CitaTratamiento registro = invocacion.getArgument(0);
            registro.setId(UUID.randomUUID());
            return registro;
        });
    }

    private CitaTratamiento registro(Cita cita, String costo) {
        return CitaTratamiento.builder()
                .id(UUID.randomUUID())
                .cita(cita)
                .tratamiento(limpieza)
                .costo(new BigDecimal(costo))
                .fechaRegistro(AHORA)
                .build();
    }

    private CitaTratamientoRequest request(BigDecimal costo, String observaciones) {
        CitaTratamientoRequest request = new CitaTratamientoRequest();
        request.setIdTratamiento(limpieza.getId());
        request.setCosto(costo);
        request.setObservaciones(observaciones);
        return request;
    }

    private Authentication autenticacionDoctor() {
        return autenticacion(doctor.getUsuario().getId(), "DOCTOR");
    }

    private Authentication autenticacion(UUID usuarioId, String rol) {
        return UsernamePasswordAuthenticationToken.authenticated(
                usuarioId.toString(), null, List.of(new SimpleGrantedAuthority("ROLE_" + rol)));
    }
}
