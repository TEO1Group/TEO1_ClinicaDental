package com.teo1.clinicadental.controller;

import com.teo1.clinicadental.model.AbonoPlan;
import com.teo1.clinicadental.model.Cita;
import com.teo1.clinicadental.model.CitaTratamiento;
import com.teo1.clinicadental.model.Cliente;
import com.teo1.clinicadental.model.Doctor;
import com.teo1.clinicadental.model.EstadoCita;
import com.teo1.clinicadental.model.EstadoPlan;
import com.teo1.clinicadental.model.EstadoUsuario;
import com.teo1.clinicadental.model.PlanTratamiento;
import com.teo1.clinicadental.model.Rol;
import com.teo1.clinicadental.model.Tratamiento;
import com.teo1.clinicadental.model.Usuario;
import com.teo1.clinicadental.repository.AbonoPlanRepository;
import com.teo1.clinicadental.repository.CitaRepository;
import com.teo1.clinicadental.repository.CitaTratamientoRepository;
import com.teo1.clinicadental.repository.ClienteRepository;
import com.teo1.clinicadental.repository.DoctorRepository;
import com.teo1.clinicadental.repository.PlanTratamientoRepository;
import com.teo1.clinicadental.repository.RolRepository;
import com.teo1.clinicadental.repository.TratamientoRepository;
import com.teo1.clinicadental.repository.UsuarioRepository;
import com.teo1.clinicadental.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReporteGananciasIntegrationTests {

    private static final String GANANCIAS = "/reportes/ganancias";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private CitaRepository citaRepository;

    @Autowired
    private TratamientoRepository tratamientoRepository;

    @Autowired
    private CitaTratamientoRepository citaTratamientoRepository;

    @Autowired
    private PlanTratamientoRepository planTratamientoRepository;

    @Autowired
    private AbonoPlanRepository abonoPlanRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private Clock clock;

    private String tokenAdmin;
    private String tokenSecretaria;
    private String tokenDoctor;
    private String tokenCliente;
    private Cliente paciente;
    private Doctor doctorA;
    private Doctor doctorB;
    private Doctor doctorSinIngresos;
    private Tratamiento tratamiento;
    private LocalDate desde;
    private LocalDate hasta;

    @BeforeEach
    void setUp() {
        tokenAdmin = token(crearUsuario(Rol.ADMIN, "Admin"));
        tokenSecretaria = token(crearUsuario(Rol.SECRETARIA, "Secretaria"));

        Usuario usuarioPaciente = crearUsuario(Rol.CLIENTE, "Alvarez");
        paciente = clienteRepository.save(Cliente.builder().usuario(usuarioPaciente).dpi(dpiAleatorio()).build());
        tokenCliente = token(usuarioPaciente);

        Usuario usuarioDoctorA = crearUsuario(Rol.DOCTOR, "Castillo");
        doctorA = doctorRepository.save(Doctor.builder().usuario(usuarioDoctorA).especialidad("Ortodoncia").build());
        tokenDoctor = token(usuarioDoctorA);
        doctorB = doctorRepository.save(Doctor.builder()
                .usuario(crearUsuario(Rol.DOCTOR, "Duarte")).especialidad("Endodoncia").build());
        doctorSinIngresos = doctorRepository.save(Doctor.builder()
                .usuario(crearUsuario(Rol.DOCTOR, "Estrada")).especialidad("Odontologia general").build());

        tratamiento = tratamientoRepository.save(Tratamiento.builder()
                .nombre("Tratamiento de prueba " + UUID.randomUUID())
                .descripcion("Solo para pruebas")
                .duracionMinutos(30)
                .costo(new BigDecimal("300.00"))
                .build());

        desde = LocalDate.now(clock).minusDays(400);
        hasta = desde.plusDays(2);
    }

    @Test
    void adminYSecretariaVenLosTotalesExactos() throws Exception {
        crearDatos();

        for (String token : new String[] {tokenAdmin, tokenSecretaria}) {
            ganancias(token, desde.toString(), hasta.toString())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.desde").value(desde.toString()))
                    .andExpect(jsonPath("$.hasta").value(hasta.toString()))
                    .andExpect(jsonPath("$.totalTratamientos").value(1650.25))
                    .andExpect(jsonPath("$.totalAbonos").value(750.0))
                    .andExpect(jsonPath(filtro(doctorA, "cantidadTratamientos")).value(3))
                    .andExpect(jsonPath(filtro(doctorA, "total")).value(650.25))
                    .andExpect(jsonPath(filtro(doctorA, "apellidoDoctor")).value("Castillo"))
                    .andExpect(jsonPath(filtro(doctorB, "cantidadTratamientos")).value(1))
                    .andExpect(jsonPath(filtro(doctorB, "total")).value(1000.0))
                    .andExpect(jsonPath(filtro(doctorSinIngresos, "cantidadTratamientos")).value(0))
                    .andExpect(jsonPath(filtro(doctorSinIngresos, "total")).value(0));
        }
    }

    @Test
    void losBordesCuentanYLoDeFueraDelRangoNo() throws Exception {
        crearDatos();

        ganancias(tokenAdmin, desde.toString(), desde.toString())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTratamientos").value(450.25))
                .andExpect(jsonPath("$.totalAbonos").value(500.0))
                .andExpect(jsonPath(filtro(doctorA, "total")).value(450.25));

        ganancias(tokenAdmin, hasta.toString(), hasta.toString())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTratamientos").value(200.0))
                .andExpect(jsonPath("$.totalAbonos").value(250.0));

        ganancias(tokenAdmin, hasta.plusDays(1).toString(), hasta.plusDays(1).toString())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTratamientos").value(999.0))
                .andExpect(jsonPath("$.totalAbonos").value(777.0));
    }

    @Test
    void unaCitaAtendidaFuturaNoCuenta() throws Exception {
        LocalDate hoy = LocalDate.now(clock);
        registrarTratamiento(crearCita(doctorSinIngresos, hoy.minusDays(1), LocalTime.of(9, 0), EstadoCita.ATENDIDA), "120.00");
        registrarTratamiento(crearCita(doctorSinIngresos, hoy.plusDays(1), LocalTime.of(9, 0), EstadoCita.ATENDIDA), "800.00");

        ganancias(tokenAdmin, hoy.minusDays(1).toString(), hoy.plusDays(3).toString())
                .andExpect(status().isOk())
                .andExpect(jsonPath(filtro(doctorSinIngresos, "cantidadTratamientos")).value(1))
                .andExpect(jsonPath(filtro(doctorSinIngresos, "total")).value(120.0));
    }

    @Test
    void parametrosInvalidosDevuelven400() throws Exception {
        mockMvc.perform(conToken(get(GANANCIAS).param("desde", desde.toString()), tokenAdmin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Debes indicar las fechas desde y hasta"));

        ganancias(tokenAdmin, hasta.toString(), desde.toString())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("La fecha desde no puede ser posterior a hasta"));

        ganancias(tokenAdmin, "01-10-2026", hasta.toString())
                .andExpect(status().isBadRequest());
    }

    @Test
    void doctorYClienteReciben403() throws Exception {
        for (String token : new String[] {tokenDoctor, tokenCliente}) {
            ganancias(token, desde.toString(), hasta.toString())
                    .andExpect(status().isForbidden());
        }
    }

    private void crearDatos() {
        Cita atendidaDesde = crearCita(doctorA, desde, LocalTime.of(9, 0), EstadoCita.ATENDIDA);
        registrarTratamiento(atendidaDesde, "300.00");
        registrarTratamiento(atendidaDesde, "150.25");
        registrarTratamiento(crearCita(doctorA, hasta, LocalTime.of(17, 0), EstadoCita.ATENDIDA), "200.00");
        registrarTratamiento(crearCita(doctorB, desde.plusDays(1), LocalTime.of(10, 0), EstadoCita.ATENDIDA), "1000.00");

        registrarTratamiento(crearCita(doctorB, desde.plusDays(1), LocalTime.of(11, 0), EstadoCita.CANCELADA), "5000.00");
        registrarTratamiento(crearCita(doctorA, desde.minusDays(1), LocalTime.of(9, 0), EstadoCita.ATENDIDA), "888.00");
        registrarTratamiento(crearCita(doctorB, hasta.plusDays(1), LocalTime.of(9, 0), EstadoCita.ATENDIDA), "999.00");

        PlanTratamiento plan = planTratamientoRepository.save(PlanTratamiento.builder()
                .cliente(paciente)
                .doctor(doctorA)
                .tratamiento(tratamiento)
                .totalSesiones(4)
                .costoTotal(new BigDecimal("4000.00"))
                .estado(EstadoPlan.ACTIVO)
                .fechaCreacion(desde.minusDays(5).atTime(8, 0))
                .build());
        registrarAbono(plan, desde.atStartOfDay(), "500.00");
        registrarAbono(plan, hasta.atTime(23, 59, 59), "250.00");
        registrarAbono(plan, hasta.plusDays(1).atStartOfDay(), "777.00");
        registrarAbono(plan, desde.minusDays(1).atTime(23, 59, 59), "666.00");
    }

    private Cita crearCita(Doctor doctor, LocalDate fecha, LocalTime hora, EstadoCita estado) {
        return citaRepository.save(Cita.builder()
                .cliente(paciente)
                .doctor(doctor)
                .fecha(fecha)
                .hora(hora)
                .estado(estado)
                .build());
    }

    private void registrarTratamiento(Cita cita, String costo) {
        citaTratamientoRepository.save(CitaTratamiento.builder()
                .cita(cita)
                .tratamiento(tratamiento)
                .costo(new BigDecimal(costo))
                .fechaRegistro(LocalDateTime.of(cita.getFecha(), cita.getHora()))
                .build());
    }

    private void registrarAbono(PlanTratamiento plan, LocalDateTime fecha, String monto) {
        abonoPlanRepository.save(AbonoPlan.builder()
                .plan(plan)
                .monto(new BigDecimal(monto))
                .fecha(fecha)
                .build());
    }

    private String filtro(Doctor doctor, String campo) {
        return "$.doctores[?(@.idDoctor == '" + doctor.getId() + "')]." + campo;
    }

    private ResultActions ganancias(String token, String desdeTexto, String hastaTexto) throws Exception {
        return mockMvc.perform(conToken(get(GANANCIAS).param("desde", desdeTexto).param("hasta", hastaTexto), token));
    }

    private Usuario crearUsuario(Rol rol, String apellido) {
        return usuarioRepository.save(Usuario.builder()
                .nombre("Prueba")
                .apellido(apellido)
                .email(rol.name().toLowerCase() + "-" + UUID.randomUUID() + "@prueba.com")
                .passwordHash("sin-uso")
                .rol(rolRepository.findByNombreRol(rol).orElseThrow())
                .estado(EstadoUsuario.ACTIVO)
                .build());
    }

    private String token(Usuario usuario) {
        return jwtService.generateToken(usuario.getId(), usuario.getRol().getNombreRol());
    }

    private MockHttpServletRequestBuilder conToken(MockHttpServletRequestBuilder request, String token) {
        return request
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON);
    }

    private String dpiAleatorio() {
        return String.format("%013d", Math.abs(UUID.randomUUID().getMostSignificantBits()) % 10_000_000_000_000L);
    }
}
