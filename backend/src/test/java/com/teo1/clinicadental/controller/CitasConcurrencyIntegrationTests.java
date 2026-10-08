package com.teo1.clinicadental.controller;

import com.teo1.clinicadental.model.Cliente;
import com.teo1.clinicadental.model.DiaSemana;
import com.teo1.clinicadental.model.Doctor;
import com.teo1.clinicadental.model.EstadoUsuario;
import com.teo1.clinicadental.model.Horario;
import com.teo1.clinicadental.model.Rol;
import com.teo1.clinicadental.model.Usuario;
import com.teo1.clinicadental.repository.CitaRepository;
import com.teo1.clinicadental.repository.ClienteRepository;
import com.teo1.clinicadental.repository.DoctorRepository;
import com.teo1.clinicadental.repository.HorarioRepository;
import com.teo1.clinicadental.repository.RolRepository;
import com.teo1.clinicadental.repository.UsuarioRepository;
import com.teo1.clinicadental.security.JwtService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CitasConcurrencyIntegrationTests {

    @LocalServerPort
    private int port;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private HorarioRepository horarioRepository;

    @Autowired
    private CitaRepository citaRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private Clock clock;

    private Doctor doctor;
    private LocalDate fecha;
    private Cliente primerCliente;
    private Cliente segundoCliente;
    private Usuario doctorUsuario;
    private Usuario primerClienteUsuario;
    private Usuario segundoClienteUsuario;

    @BeforeEach
    void setUp() {
        doctorUsuario = crearUsuario(Rol.DOCTOR);
        doctor = doctorRepository.save(Doctor.builder()
                .usuario(doctorUsuario)
                .especialidad("Ortodoncia")
                .build());

        primerClienteUsuario = crearUsuario(Rol.CLIENTE);
        primerCliente = clienteRepository.save(Cliente.builder()
                .usuario(primerClienteUsuario)
                .dpi(dpiAleatorio())
                .build());

        segundoClienteUsuario = crearUsuario(Rol.CLIENTE);
        segundoCliente = clienteRepository.save(Cliente.builder()
                .usuario(segundoClienteUsuario)
                .dpi(dpiAleatorio())
                .build());

        fecha = LocalDate.now(clock).plusDays(14);
        horarioRepository.save(Horario.builder()
                .doctor(doctor)
                .diaSemana(DiaSemana.desde(fecha.getDayOfWeek()))
                .horaInicio(LocalTime.of(8, 0))
                .horaFin(LocalTime.of(12, 0))
                .build());
    }

    @AfterEach
    void cleanUp() {
        if (doctor == null) {
            return;
        }
        citaRepository.deleteAll(citaRepository.findByDoctorIdAndFechaAndEstadoNot(
                doctor.getId(), fecha, com.teo1.clinicadental.model.EstadoCita.CANCELADA));
        horarioRepository.deleteAll(horarioRepository.findByDoctorIdOrderByDiaSemanaAscHoraInicioAsc(doctor.getId()));
        doctorRepository.delete(doctor);
        clienteRepository.delete(primerCliente);
        clienteRepository.delete(segundoCliente);
        usuarioRepository.deleteAll(List.of(doctorUsuario, primerClienteUsuario, segundoClienteUsuario));
    }

    @Test
    void concurrentOverlappingStartsOnlyAllowOneReservation() throws Exception {
        List<HttpStatus> results = reserveTogether("09:00", "09:15");

        assertThat(results).containsExactlyInAnyOrder(HttpStatus.CREATED, HttpStatus.CONFLICT);
    }

    @Test
    void concurrentAdjacentReservationsBothSucceed() throws Exception {
        List<HttpStatus> results = reserveTogether("09:00", "09:30");

        assertThat(results).containsExactly(HttpStatus.CREATED, HttpStatus.CREATED);
    }

    @Test
    void openApiShowsOnlyFinalStatesForPatchAndAllStatesInResponses() throws Exception {
        HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/v3/api-docs")).GET().build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"enum\":[\"ATENDIDA\",\"CANCELADA\",\"NO_ASISTIO\"]");
        assertThat(response.body()).contains("\"enum\":[\"AGENDADA\",\"ATENDIDA\",\"CANCELADA\",\"NO_ASISTIO\"]");
    }

    private List<HttpStatus> reserveTogether(String firstTime, String secondTime) throws Exception {
        CyclicBarrier startTogether = new CyclicBarrier(2);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<HttpStatus> first = executor.submit(() -> reserveAt(startTogether, primerCliente, firstTime));
            Future<HttpStatus> second = executor.submit(() -> reserveAt(startTogether, segundoCliente, secondTime));
            return List.of(first.get(), second.get());
        }
    }

    private HttpStatus reserveAt(CyclicBarrier startTogether, Cliente cliente, String hora) throws Exception {
        startTogether.await();
        String token = jwtService.generateToken(cliente.getUsuario().getId(), Rol.CLIENTE);
        String body = "{\"idDoctor\":\"" + doctor.getId() + "\",\"fecha\":\"" + fecha
                + "\",\"hora\":\"" + hora + "\"}";
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/citas"))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<Void> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.discarding());
        return HttpStatus.valueOf(response.statusCode());
    }

    private Usuario crearUsuario(Rol rol) {
        return usuarioRepository.save(Usuario.builder()
                .nombre("Prueba")
                .apellido(rol.name())
                .email(rol.name().toLowerCase() + "-" + UUID.randomUUID() + "@prueba.com")
                .passwordHash("unused-test-hash")
                .rol(rolRepository.findByNombreRol(rol).orElseThrow())
                .estado(EstadoUsuario.ACTIVO)
                .build());
    }

    private String dpiAleatorio() {
        return String.format("%013d", Math.abs(UUID.randomUUID().getMostSignificantBits()) % 10_000_000_000_000L);
    }
}
