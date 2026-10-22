package com.teo1.clinicadental.controller;

import com.teo1.clinicadental.model.Cliente;
import com.teo1.clinicadental.model.Doctor;
import com.teo1.clinicadental.model.EstadoPlan;
import com.teo1.clinicadental.model.EstadoUsuario;
import com.teo1.clinicadental.model.PlanTratamiento;
import com.teo1.clinicadental.model.Rol;
import com.teo1.clinicadental.model.Usuario;
import com.teo1.clinicadental.repository.AbonoPlanRepository;
import com.teo1.clinicadental.repository.ClienteRepository;
import com.teo1.clinicadental.repository.DoctorRepository;
import com.teo1.clinicadental.repository.PlanTratamientoRepository;
import com.teo1.clinicadental.repository.RolRepository;
import com.teo1.clinicadental.repository.TratamientoRepository;
import com.teo1.clinicadental.repository.UsuarioRepository;
import com.teo1.clinicadental.security.JwtService;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AbonosConcurrencyIntegrationTests {

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
    private TratamientoRepository tratamientoRepository;
    @Autowired
    private PlanTratamientoRepository planRepository;
    @Autowired
    private AbonoPlanRepository abonoRepository;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private Clock clock;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private PlatformTransactionManager transactionManager;

    private List<Usuario> usuarios;
    private PlanTratamiento plan;

    @BeforeEach
    void setUp() {
        usuarios = List.of(usuario(Rol.ADMIN), usuario(Rol.SECRETARIA), usuario(Rol.CLIENTE), usuario(Rol.DOCTOR));
        Cliente cliente = clienteRepository.save(Cliente.builder().usuario(usuarios.get(2))
                .dpi(String.format("%013d", Math.floorMod(UUID.randomUUID().getMostSignificantBits(), 10_000_000_000_000L)))
                .build());
        Doctor doctor = doctorRepository.save(Doctor.builder().usuario(usuarios.get(3)).especialidad("Prueba").build());
        plan = planRepository.save(PlanTratamiento.builder().cliente(cliente).doctor(doctor)
                .tratamiento(tratamientoRepository.findAllByOrderByNombreAsc().get(0))
                .totalSesiones(2).costoTotal(new BigDecimal("100.00")).estado(EstadoPlan.ACTIVO)
                .fechaCreacion(LocalDateTime.now(clock)).build());
    }

    @AfterEach
    void cleanUp() {
        if (usuarios != null) {
            usuarioRepository.deleteAll(usuarios);
        }
    }

    @Test
    void simultaneousPaymentsCannotExceedRemainingBalance() throws Exception {
        assertThat(payTogether("60.00", "60.00")).containsExactlyInAnyOrder(201, 409);
        assertThat(abonoRepository.sumarPorPlan(List.of(plan.getId())).get(0).getTotal())
                .isEqualByComparingTo("60.00");
        assertThat(abonoRepository.findByPlanIdOrderByFechaAscIdAsc(plan.getId())).hasSize(1);
    }

    @Test
    void simultaneousPaymentsWithinBalanceBothSucceed() throws Exception {
        assertThat(payTogether("40.00", "60.00")).containsExactly(201, 201);
        assertThat(abonoRepository.sumarPorPlan(List.of(plan.getId())).get(0).getTotal())
                .isEqualByComparingTo("100.00");
        assertThat(abonoRepository.findByPlanIdOrderByFechaAscIdAsc(plan.getId())).hasSize(2);
    }

    private List<Integer> payTogether(String firstAmount, String secondAmount) throws Exception {
        CyclicBarrier start = new CyclicBarrier(2);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            List<Future<Integer>> futures = new ArrayList<>();
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                planRepository.findByIdForUpdate(plan.getId()).orElseThrow();
                futures.add(executor.submit(() -> pay(start, usuarios.get(0), firstAmount)));
                futures.add(executor.submit(() -> pay(start, usuarios.get(1), secondAmount)));
                // Both HTTP transactions must actually wait on this PostgreSQL row lock before release.
                long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
                int blocked;
                do {
                    jdbc.execute("select pg_stat_clear_snapshot()");
                    blocked = jdbc.queryForObject("select count(*) from pg_stat_activity "
                            + "where datname = current_database() and cardinality(pg_blocking_pids(pid)) > 0 "
                            + "and query like '%plan_tratamiento%'", Integer.class);
                    if (blocked == 2) {
                        break;
                    }
                    LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(20));
                } while (System.nanoTime() < deadline);
                assertThat(blocked).as("Both payment transactions acquire the plan lock before reading balance").isEqualTo(2);
            });
            return List.of(futures.get(0).get(20, TimeUnit.SECONDS), futures.get(1).get(20, TimeUnit.SECONDS));
        }
    }

    private int pay(CyclicBarrier start, Usuario usuario, String amount) throws Exception {
        start.await(10, TimeUnit.SECONDS);
        String token = jwtService.generateToken(usuario.getId(), usuario.getRol().getNombreRol());
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/planes/" + plan.getId() + "/abonos"))
                .timeout(Duration.ofSeconds(15))
                .header("Authorization", "Bearer " + token).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"monto\":" + amount + "}")).build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
    }

    private Usuario usuario(Rol rol) {
        return usuarioRepository.save(Usuario.builder().nombre("Prueba").apellido(rol.name())
                .email(UUID.randomUUID() + "@prueba.com").passwordHash("unused-test-hash")
                .rol(rolRepository.findByNombreRol(rol).orElseThrow()).estado(EstadoUsuario.ACTIVO).build());
    }
}
