package com.teo1.clinicadental.service;

import com.teo1.clinicadental.dto.CitaRequest;
import com.teo1.clinicadental.dto.CitaResponse;
import com.teo1.clinicadental.dto.EstadoCitaRequest;
import com.teo1.clinicadental.model.Cita;
import com.teo1.clinicadental.model.Cliente;
import com.teo1.clinicadental.model.DiaSemana;
import com.teo1.clinicadental.model.Doctor;
import com.teo1.clinicadental.model.EstadoCita;
import com.teo1.clinicadental.model.EstadoUsuario;
import com.teo1.clinicadental.model.Horario;
import com.teo1.clinicadental.model.Usuario;
import com.teo1.clinicadental.repository.CitaRepository;
import com.teo1.clinicadental.repository.ClienteRepository;
import com.teo1.clinicadental.repository.DoctorRepository;
import com.teo1.clinicadental.repository.HorarioRepository;
import java.time.Clock;
import java.time.LocalDate;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CitaServiceTests {

    private static final ZoneId ZONA = ZoneId.of("America/Guatemala");
    private static final LocalDate FECHA = LocalDate.of(2026, 10, 15);
    private static final DiaSemana DIA = DiaSemana.desde(FECHA.getDayOfWeek());

    @Mock
    private CitaRepository citaRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private DoctorRepository doctorRepository;

    @Mock
    private HorarioRepository horarioRepository;

    private CitaService citaService;
    private Doctor doctor;
    private Cliente cliente;
    private UUID usuarioClienteId;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(ZonedDateTime.of(2026, 10, 8, 8, 0, 0, 0, ZONA).toInstant(), ZONA);
        citaService = new CitaService(
                citaRepository, clienteRepository, doctorRepository, horarioRepository, new CitaMapper(), clock, 30);

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
    void citaValidaQuedaAgendada() {
        prepararAgendamiento(List.of());

        CitaResponse respuesta = citaService.crearCita(request(LocalTime.of(9, 0)), autenticacion("CLIENTE"));

        ArgumentCaptor<Cita> captor = ArgumentCaptor.forClass(Cita.class);
        verify(citaRepository).saveAndFlush(captor.capture());
        assertEquals(EstadoCita.AGENDADA, captor.getValue().getEstado());
        assertFalse(captor.getValue().isRecordatorioEnviado());
        assertEquals(EstadoCita.AGENDADA, respuesta.getEstado());
        assertEquals(cliente.getId(), respuesta.getIdCliente());
        assertEquals(doctor.getId(), respuesta.getIdDoctor());
        assertEquals(LocalTime.of(9, 0), respuesta.getHora());
        assertEquals("Ana", respuesta.getNombreCliente());
        assertEquals("Perez", respuesta.getApellidoDoctor());
    }

    @Test
    void mismaHoraYMismoDoctorDevuelveConflicto() {
        prepararSolape(List.of(citaExistente(LocalTime.of(9, 0), EstadoCita.AGENDADA)));

        assertEstado(HttpStatus.CONFLICT, () -> citaService.crearCita(request(LocalTime.of(9, 0)), autenticacion("CLIENTE")));
        verify(citaRepository, never()).saveAndFlush(any());
    }

    @Test
    void traslapeConOtraHoraDeInicioDevuelveConflicto() {
        prepararSolape(List.of(citaExistente(LocalTime.of(9, 0), EstadoCita.AGENDADA)));

        assertEstado(HttpStatus.CONFLICT, () -> citaService.crearCita(request(LocalTime.of(9, 15)), autenticacion("CLIENTE")));
        verify(citaRepository, never()).saveAndFlush(any());
    }

    @Test
    void citaQueEmpiezaCuandoTerminaOtraSeAcepta() {
        prepararAgendamiento(List.of(citaExistente(LocalTime.of(9, 0), EstadoCita.AGENDADA)));

        CitaResponse respuesta = citaService.crearCita(request(LocalTime.of(9, 30)), autenticacion("CLIENTE"));

        assertEquals(EstadoCita.AGENDADA, respuesta.getEstado());
    }

    @Test
    void citaAntesDelInicioDelHorarioDevuelveConflicto() {
        prepararHorario();

        assertEstado(HttpStatus.CONFLICT, () -> citaService.crearCita(request(LocalTime.of(7, 45)), autenticacion("CLIENTE")));
    }

    @Test
    void citaQueTerminaDespuesDelFinDelHorarioDevuelveConflicto() {
        prepararHorario();

        assertEstado(HttpStatus.CONFLICT, () -> citaService.crearCita(request(LocalTime.of(11, 45)), autenticacion("CLIENTE")));
    }

    @Test
    void diaSinHorarioDevuelveConflicto() {
        prepararPacienteYDoctor();
        when(horarioRepository.findByDoctorIdAndDiaSemana(doctor.getId(), DIA)).thenReturn(List.of());

        assertEstado(HttpStatus.CONFLICT, () -> citaService.crearCita(request(LocalTime.of(9, 0)), autenticacion("CLIENTE")));
    }

    @Test
    void citaCanceladaNoBloqueaElHorario() {
        prepararAgendamiento(List.of());

        CitaResponse respuesta = citaService.crearCita(request(LocalTime.of(9, 0)), autenticacion("CLIENTE"));

        verify(citaRepository).findByDoctorIdAndFechaAndEstadoNot(doctor.getId(), FECHA, EstadoCita.CANCELADA);
        assertEquals(EstadoCita.AGENDADA, respuesta.getEstado());
    }

    @Test
    void pacienteEnListaNegraDevuelveProhibido() {
        cliente.setEnListaNegra(true);
        prepararPacienteYDoctor();

        assertEstado(HttpStatus.FORBIDDEN, () -> citaService.crearCita(request(LocalTime.of(9, 0)), autenticacion("CLIENTE")));
    }

    @Test
    void fechaYHoraPasadasDevuelvenSolicitudInvalida() {
        prepararPacienteYDoctor();
        CitaRequest request = request(LocalTime.of(7, 0));
        request.setFecha(LocalDate.of(2026, 10, 8));

        assertEstado(HttpStatus.BAD_REQUEST, () -> citaService.crearCita(request, autenticacion("CLIENTE")));
    }

    @Test
    void adminSinIdClienteDevuelveSolicitudInvalida() {
        assertEstado(HttpStatus.BAD_REQUEST, () -> citaService.crearCita(request(LocalTime.of(9, 0)), autenticacion("ADMIN")));
    }

    @Test
    void doctorNoPuedeAgendar() {
        assertEstado(HttpStatus.FORBIDDEN, () -> citaService.crearCita(request(LocalTime.of(9, 0)), autenticacion("DOCTOR")));
    }

    @Test
    void clienteSinPacienteAsociadoDevuelveNoEncontrado() {
        when(clienteRepository.findByUsuarioId(usuarioClienteId)).thenReturn(Optional.empty());

        assertEstado(HttpStatus.NOT_FOUND, () -> citaService.crearCita(request(LocalTime.of(9, 0)), autenticacion("CLIENTE")));
    }

    @Test
    void violacionDelIndiceUnicoDevuelveConflicto() {
        prepararSolape(List.of());
        when(citaRepository.saveAndFlush(any(Cita.class))).thenThrow(new DataIntegrityViolationException(
                "could not execute statement",
                new RuntimeException("duplicate key value violates unique constraint \"uq_cita_doctor_slot\"")));

        assertEstado(HttpStatus.CONFLICT, () -> citaService.crearCita(request(LocalTime.of(9, 0)), autenticacion("CLIENTE")));
    }

    @Test
    void secretariaYAdminVenTodasLasCitas() {
        List<Cita> todas = List.of(citaExistente(LocalTime.of(9, 0), EstadoCita.AGENDADA), citaDeOtros());
        when(citaRepository.findAllByOrderByFechaAscHoraAsc()).thenReturn(todas);

        assertEquals(2, citaService.listarCitas(null, null, null, null, autenticacion("SECRETARIA")).size());
        assertEquals(2, citaService.listarCitas(null, null, null, null, autenticacion("ADMIN")).size());
    }

    @Test
    void clienteVeSoloSusCitas() {
        when(clienteRepository.findByUsuarioId(usuarioClienteId)).thenReturn(Optional.of(cliente));
        when(citaRepository.findByClienteIdOrderByFechaAscHoraAsc(cliente.getId()))
                .thenReturn(List.of(citaExistente(LocalTime.of(9, 0), EstadoCita.AGENDADA)));

        List<CitaResponse> citas = citaService.listarCitas(null, null, null, null, autenticacion("CLIENTE"));

        assertEquals(1, citas.size());
        assertEquals(cliente.getId(), citas.get(0).getIdCliente());
        verify(citaRepository, never()).findAllByOrderByFechaAscHoraAsc();
    }

    @Test
    void doctorVeSoloSusCitas() {
        when(doctorRepository.findByUsuarioId(doctor.getUsuario().getId())).thenReturn(Optional.of(doctor));
        when(citaRepository.findByDoctorIdOrderByFechaAscHoraAsc(doctor.getId()))
                .thenReturn(List.of(citaExistente(LocalTime.of(9, 0), EstadoCita.AGENDADA)));

        List<CitaResponse> citas = citaService.listarCitas(
                null, null, null, null, autenticacion(doctor.getUsuario().getId(), "DOCTOR"));

        assertEquals(1, citas.size());
        assertEquals(doctor.getId(), citas.get(0).getIdDoctor());
        verify(citaRepository, never()).findAllByOrderByFechaAscHoraAsc();
    }

    @Test
    void clienteSinPacienteAsociadoNoVeCitas() {
        when(clienteRepository.findByUsuarioId(usuarioClienteId)).thenReturn(Optional.empty());

        assertTrue(citaService.listarCitas(null, null, null, null, autenticacion("CLIENTE")).isEmpty());
    }

    @Test
    void filtrosDelListadoSeCombinan() {
        Cita buscada = citaExistente(LocalTime.of(9, 0), EstadoCita.AGENDADA);
        Cita cancelada = citaExistente(LocalTime.of(10, 0), EstadoCita.CANCELADA);
        Cita otroDia = citaExistente(LocalTime.of(9, 0), EstadoCita.AGENDADA);
        otroDia.setFecha(FECHA.plusDays(1));
        when(citaRepository.findAllByOrderByFechaAscHoraAsc()).thenReturn(List.of(buscada, cancelada, otroDia, citaDeOtros()));

        List<CitaResponse> citas = citaService.listarCitas(
                FECHA, doctor.getId(), cliente.getId(), EstadoCita.AGENDADA, autenticacion("SECRETARIA"));

        assertEquals(1, citas.size());
        assertEquals(buscada.getId(), citas.get(0).getIdCita());
    }

    @Test
    void obtenerCitaInexistenteDevuelveNoEncontrado() {
        UUID id = UUID.randomUUID();
        when(citaRepository.findConRelacionesById(id)).thenReturn(Optional.empty());

        assertEstado(HttpStatus.NOT_FOUND, () -> citaService.obtenerCita(id, autenticacion("ADMIN")));
    }

    @Test
    void clienteObtieneSuPropiaCita() {
        Cita cita = citaExistente(LocalTime.of(9, 0), EstadoCita.AGENDADA);
        when(citaRepository.findConRelacionesById(cita.getId())).thenReturn(Optional.of(cita));

        assertEquals(cita.getId(), citaService.obtenerCita(cita.getId(), autenticacion("CLIENTE")).getIdCita());
    }

    @Test
    void clienteNoPuedeVerCitaAjena() {
        Cita ajena = citaDeOtros();
        when(citaRepository.findConRelacionesById(ajena.getId())).thenReturn(Optional.of(ajena));

        assertEstado(HttpStatus.FORBIDDEN, () -> citaService.obtenerCita(ajena.getId(), autenticacion("CLIENTE")));
    }

    @Test
    void doctorNoPuedeVerCitaDeOtroDoctor() {
        Cita ajena = citaDeOtros();
        when(citaRepository.findConRelacionesById(ajena.getId())).thenReturn(Optional.of(ajena));

        assertEstado(HttpStatus.FORBIDDEN, () -> citaService.obtenerCita(
                ajena.getId(), autenticacion(doctor.getUsuario().getId(), "DOCTOR")));
    }

    @Test
    void secretariaPuedeVerCualquierCita() {
        Cita ajena = citaDeOtros();
        when(citaRepository.findConRelacionesById(ajena.getId())).thenReturn(Optional.of(ajena));

        assertEquals(ajena.getId(), citaService.obtenerCita(ajena.getId(), autenticacion("SECRETARIA")).getIdCita());
    }

    @Test
    void doctorMarcaSuCitaComoAtendida() {
        Cita cita = prepararCambioEstado(EstadoCita.AGENDADA);

        CitaResponse respuesta = citaService.cambiarEstado(
                cita.getId(), estado(EstadoCita.ATENDIDA), autenticacion(doctor.getUsuario().getId(), "DOCTOR"));

        assertEquals(EstadoCita.ATENDIDA, respuesta.getEstado());
        verify(citaRepository).save(cita);
    }

    @Test
    void doctorMarcaSuCitaComoNoAsistio() {
        Cita cita = prepararCambioEstado(EstadoCita.AGENDADA);

        CitaResponse respuesta = citaService.cambiarEstado(
                cita.getId(), estado(EstadoCita.NO_ASISTIO), autenticacion(doctor.getUsuario().getId(), "DOCTOR"));

        assertEquals(EstadoCita.NO_ASISTIO, respuesta.getEstado());
    }

    @Test
    void clienteCancelaSuCita() {
        Cita cita = prepararCambioEstado(EstadoCita.AGENDADA);

        CitaResponse respuesta = citaService.cambiarEstado(cita.getId(), estado(EstadoCita.CANCELADA), autenticacion("CLIENTE"));

        assertEquals(EstadoCita.CANCELADA, respuesta.getEstado());
    }

    @Test
    void secretariaCancelaCualquierCita() {
        Cita cita = prepararCambioEstado(EstadoCita.AGENDADA);

        CitaResponse respuesta = citaService.cambiarEstado(cita.getId(), estado(EstadoCita.CANCELADA), autenticacion("SECRETARIA"));

        assertEquals(EstadoCita.CANCELADA, respuesta.getEstado());
    }

    @Test
    void adminPuedeCambiarACualquierEstadoFinal() {
        for (EstadoCita nuevo : List.of(EstadoCita.ATENDIDA, EstadoCita.CANCELADA, EstadoCita.NO_ASISTIO)) {
            Cita cita = prepararCambioEstado(EstadoCita.AGENDADA);

            assertEquals(nuevo, citaService.cambiarEstado(cita.getId(), estado(nuevo), autenticacion("ADMIN")).getEstado());
        }
    }

    @Test
    void estadosFinalesNoPuedenCambiar() {
        for (EstadoCita actual : List.of(EstadoCita.ATENDIDA, EstadoCita.CANCELADA, EstadoCita.NO_ASISTIO)) {
            Cita cita = citaExistente(LocalTime.of(9, 0), actual);
            when(citaRepository.findConRelacionesById(cita.getId())).thenReturn(Optional.of(cita));

            assertEstado(HttpStatus.CONFLICT, () -> citaService.cambiarEstado(
                    cita.getId(), estado(EstadoCita.CANCELADA), autenticacion("ADMIN")));
        }
        verify(citaRepository, never()).save(any());
    }

    @Test
    void pedirAgendadaDevuelveSolicitudInvalida() {
        assertEstado(HttpStatus.BAD_REQUEST, () -> citaService.cambiarEstado(
                UUID.randomUUID(), estado(EstadoCita.AGENDADA), autenticacion("ADMIN")));
    }

    @Test
    void cambiarEstadoDeCitaInexistenteDevuelveNoEncontrado() {
        UUID id = UUID.randomUUID();
        when(citaRepository.findConRelacionesById(id)).thenReturn(Optional.empty());

        assertEstado(HttpStatus.NOT_FOUND, () -> citaService.cambiarEstado(id, estado(EstadoCita.CANCELADA), autenticacion("ADMIN")));
    }

    @Test
    void clienteNoPuedeCancelarCitaAjena() {
        Cita ajena = citaDeOtros();
        when(citaRepository.findConRelacionesById(ajena.getId())).thenReturn(Optional.of(ajena));

        assertEstado(HttpStatus.FORBIDDEN, () -> citaService.cambiarEstado(
                ajena.getId(), estado(EstadoCita.CANCELADA), autenticacion("CLIENTE")));
    }

    @Test
    void clienteNoPuedeMarcarSuCitaComoAtendida() {
        Cita cita = citaExistente(LocalTime.of(9, 0), EstadoCita.AGENDADA);
        when(citaRepository.findConRelacionesById(cita.getId())).thenReturn(Optional.of(cita));

        assertEstado(HttpStatus.FORBIDDEN, () -> citaService.cambiarEstado(
                cita.getId(), estado(EstadoCita.ATENDIDA), autenticacion("CLIENTE")));
    }

    @Test
    void secretariaNoPuedeMarcarNoAsistio() {
        Cita cita = citaExistente(LocalTime.of(9, 0), EstadoCita.AGENDADA);
        when(citaRepository.findConRelacionesById(cita.getId())).thenReturn(Optional.of(cita));

        assertEstado(HttpStatus.FORBIDDEN, () -> citaService.cambiarEstado(
                cita.getId(), estado(EstadoCita.NO_ASISTIO), autenticacion("SECRETARIA")));
    }

    @Test
    void doctorNoPuedeCancelar() {
        Cita cita = citaExistente(LocalTime.of(9, 0), EstadoCita.AGENDADA);
        when(citaRepository.findConRelacionesById(cita.getId())).thenReturn(Optional.of(cita));

        assertEstado(HttpStatus.FORBIDDEN, () -> citaService.cambiarEstado(
                cita.getId(), estado(EstadoCita.CANCELADA), autenticacion(doctor.getUsuario().getId(), "DOCTOR")));
    }

    @Test
    void doctorNoPuedeMarcarCitaDeOtroDoctor() {
        Cita ajena = citaDeOtros();
        when(citaRepository.findConRelacionesById(ajena.getId())).thenReturn(Optional.of(ajena));

        assertEstado(HttpStatus.FORBIDDEN, () -> citaService.cambiarEstado(
                ajena.getId(), estado(EstadoCita.ATENDIDA), autenticacion(doctor.getUsuario().getId(), "DOCTOR")));
        verify(citaRepository, never()).save(any());
    }

    private void prepararPacienteYDoctor() {
        when(clienteRepository.findByUsuarioId(usuarioClienteId)).thenReturn(Optional.of(cliente));
        when(doctorRepository.findById(doctor.getId())).thenReturn(Optional.of(doctor));
    }

    private void prepararHorario() {
        prepararPacienteYDoctor();
        when(horarioRepository.findByDoctorIdAndDiaSemana(doctor.getId(), DIA)).thenReturn(List.of(Horario.builder()
                .id(UUID.randomUUID())
                .doctor(doctor)
                .diaSemana(DIA)
                .horaInicio(LocalTime.of(8, 0))
                .horaFin(LocalTime.of(12, 0))
                .build()));
    }

    private void prepararSolape(List<Cita> existentes) {
        prepararHorario();
        when(citaRepository.findByDoctorIdAndFechaAndEstadoNot(doctor.getId(), FECHA, EstadoCita.CANCELADA))
                .thenReturn(existentes);
    }

    private void prepararAgendamiento(List<Cita> existentes) {
        prepararSolape(existentes);
        when(citaRepository.saveAndFlush(any(Cita.class))).thenAnswer(invocacion -> {
            Cita cita = invocacion.getArgument(0);
            cita.setId(UUID.randomUUID());
            return cita;
        });
    }

    private Cita citaExistente(LocalTime hora, EstadoCita estado) {
        return Cita.builder()
                .id(UUID.randomUUID())
                .cliente(cliente)
                .doctor(doctor)
                .fecha(FECHA)
                .hora(hora)
                .estado(estado)
                .build();
    }

    private Cita citaDeOtros() {
        Doctor otroDoctor = Doctor.builder()
                .id(UUID.randomUUID())
                .usuario(usuario(UUID.randomUUID(), "Laura", "Diaz"))
                .especialidad("Ortodoncia")
                .build();
        Cliente otroCliente = Cliente.builder()
                .id(UUID.randomUUID())
                .usuario(usuario(UUID.randomUUID(), "Pedro", "Gomez"))
                .dpi("9876543210101")
                .build();
        return Cita.builder()
                .id(UUID.randomUUID())
                .cliente(otroCliente)
                .doctor(otroDoctor)
                .fecha(FECHA)
                .hora(LocalTime.of(9, 0))
                .estado(EstadoCita.AGENDADA)
                .build();
    }

    private Cita prepararCambioEstado(EstadoCita actual) {
        Cita cita = citaExistente(LocalTime.of(9, 0), actual);
        when(citaRepository.findConRelacionesById(cita.getId())).thenReturn(Optional.of(cita));
        when(citaRepository.save(cita)).thenReturn(cita);
        return cita;
    }

    private EstadoCitaRequest estado(EstadoCita estado) {
        EstadoCitaRequest request = new EstadoCitaRequest();
        request.setEstado(estado);
        return request;
    }

    private CitaRequest request(LocalTime hora) {
        CitaRequest request = new CitaRequest();
        request.setIdDoctor(doctor.getId());
        request.setFecha(FECHA);
        request.setHora(hora);
        request.setNotas("Limpieza dental");
        return request;
    }

    private Authentication autenticacion(String rol) {
        String usuarioId = "CLIENTE".equals(rol) ? usuarioClienteId.toString() : UUID.randomUUID().toString();
        return UsernamePasswordAuthenticationToken.authenticated(
                usuarioId, null, List.of(new SimpleGrantedAuthority("ROLE_" + rol)));
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
                .estado(EstadoUsuario.ACTIVO)
                .build();
    }

    private void assertEstado(HttpStatus esperado, Runnable accion) {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, accion::run);
        assertEquals(esperado, exception.getStatusCode());
    }
}
