package com.teo1.clinicadental.controller;

import com.teo1.clinicadental.model.Cita;
import com.teo1.clinicadental.model.CitaTratamiento;
import com.teo1.clinicadental.model.Cliente;
import com.teo1.clinicadental.model.Doctor;
import com.teo1.clinicadental.model.EstadoCita;
import com.teo1.clinicadental.model.EstadoUsuario;
import com.teo1.clinicadental.model.Rol;
import com.teo1.clinicadental.model.Tratamiento;
import com.teo1.clinicadental.model.Usuario;
import com.teo1.clinicadental.repository.CitaRepository;
import com.teo1.clinicadental.repository.CitaTratamientoRepository;
import com.teo1.clinicadental.repository.ClienteRepository;
import com.teo1.clinicadental.repository.DoctorRepository;
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
class ReportesInasistenciasIngresosIntegrationTests {

    private static final String INASISTENCIAS = "/reportes/inasistencias";
    private static final String INGRESOS = "/reportes/ingresos-por-tratamiento";

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
    private JwtService jwtService;

    @Autowired
    private Clock clock;

    private String tokenAdmin;
    private String tokenSecretaria;
    private String tokenDoctor;
    private String tokenCliente;
    private Doctor doctorA;
    private Doctor doctorB;
    private Cliente pacienteX;
    private Cliente pacienteY;
    private LocalDate desde;
    private LocalDate hasta;

    @BeforeEach
    void setUp() {
        tokenAdmin = token(crearUsuario(Rol.ADMIN, "Admin"));
        tokenSecretaria = token(crearUsuario(Rol.SECRETARIA, "Secretaria"));

        Usuario usuarioX = crearUsuario(Rol.CLIENTE, "Alvarez");
        pacienteX = clienteRepository.save(Cliente.builder().usuario(usuarioX).dpi(dpiAleatorio()).build());
        tokenCliente = token(usuarioX);
        pacienteY = clienteRepository.save(Cliente.builder()
                .usuario(crearUsuario(Rol.CLIENTE, "Barrios")).dpi(dpiAleatorio()).build());

        Usuario usuarioDoctorA = crearUsuario(Rol.DOCTOR, "Castillo");
        doctorA = doctorRepository.save(Doctor.builder().usuario(usuarioDoctorA).especialidad("Ortodoncia").build());
        tokenDoctor = token(usuarioDoctorA);
        doctorB = doctorRepository.save(Doctor.builder()
                .usuario(crearUsuario(Rol.DOCTOR, "Duarte")).especialidad("Endodoncia").build());

        desde = LocalDate.now(clock).plusDays(30);
        hasta = desde.plusDays(2);
    }

    @Test
    void inasistenciasYCancelacionesPorDoctorYPorPaciente() throws Exception {
        crearDatosDeInasistencias();

        for (String token : new String[] {tokenAdmin, tokenSecretaria}) {
            inasistencias(token, desde, hasta)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.desde").value(desde.toString()))
                    .andExpect(jsonPath("$.hasta").value(hasta.toString()))
                    .andExpect(jsonPath("$.totalNoAsistio").value(2))
                    .andExpect(jsonPath("$.totalCanceladas").value(3))
                    .andExpect(jsonPath("$.total").value(5))
                    .andExpect(jsonPath("$.doctores.length()").value(2))
                    .andExpect(jsonPath("$.doctores[0].idDoctor").value(doctorA.getId().toString()))
                    .andExpect(jsonPath("$.doctores[0].apellidoDoctor").value("Castillo"))
                    .andExpect(jsonPath("$.doctores[0].noAsistio").value(2))
                    .andExpect(jsonPath("$.doctores[0].canceladas").value(1))
                    .andExpect(jsonPath("$.doctores[0].total").value(3))
                    .andExpect(jsonPath("$.doctores[1].idDoctor").value(doctorB.getId().toString()))
                    .andExpect(jsonPath("$.doctores[1].noAsistio").value(0))
                    .andExpect(jsonPath("$.doctores[1].canceladas").value(2))
                    .andExpect(jsonPath("$.pacientes.length()").value(2))
                    .andExpect(jsonPath("$.pacientes[0].idCliente").value(pacienteY.getId().toString()))
                    .andExpect(jsonPath("$.pacientes[0].apellidoCliente").value("Barrios"))
                    .andExpect(jsonPath("$.pacientes[0].noAsistio").value(1))
                    .andExpect(jsonPath("$.pacientes[0].canceladas").value(2))
                    .andExpect(jsonPath("$.pacientes[0].total").value(3))
                    .andExpect(jsonPath("$.pacientes[1].idCliente").value(pacienteX.getId().toString()))
                    .andExpect(jsonPath("$.pacientes[1].total").value(2));
        }
    }

    @Test
    void inasistenciasRespetaLosBordesDelRango() throws Exception {
        crearDatosDeInasistencias();

        inasistencias(tokenAdmin, desde, desde)
                .andExpect(jsonPath("$.totalNoAsistio").value(1))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.pacientes[0].idCliente").value(pacienteX.getId().toString()));
        inasistencias(tokenAdmin, hasta, hasta)
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.pacientes[0].idCliente").value(pacienteY.getId().toString()));
        inasistencias(tokenAdmin, desde.minusDays(1), hasta.plusDays(1))
                .andExpect(jsonPath("$.totalNoAsistio").value(3))
                .andExpect(jsonPath("$.totalCanceladas").value(4))
                .andExpect(jsonPath("$.total").value(7));
        inasistencias(tokenAdmin, hasta.plusYears(10), hasta.plusYears(10))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.doctores").isEmpty())
                .andExpect(jsonPath("$.pacientes").isEmpty());
    }

    @Test
    void ingresosPorTratamientoSumanLoCobradoEnElPeriodo() throws Exception {
        Tratamiento limpieza = crearTratamiento("Limpieza", "300.00");
        Tratamiento endodoncia = crearTratamiento("Endodoncia", "1800.00");
        crearTratamiento("Sin registros", "100.00");

        Cita primerDia = crearCita(doctorA, pacienteX, desde, "09:00", EstadoCita.ATENDIDA);
        registrar(primerDia, limpieza, "250.00");
        registrar(primerDia, limpieza, "300.00");
        registrar(primerDia, endodoncia, "1800.00");
        registrar(crearCita(doctorB, pacienteY, hasta, "09:00", EstadoCita.ATENDIDA), endodoncia, "1500.00");
        registrar(crearCita(doctorA, pacienteY, desde.minusDays(1), "09:00", EstadoCita.ATENDIDA), limpieza, "999.00");

        for (String token : new String[] {tokenAdmin, tokenSecretaria}) {
            ingresos(token, desde, hasta)
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.cantidadTotal").value(4))
                    .andExpect(jsonPath("$.ingresoTotal").value(3850.00))
                    .andExpect(jsonPath("$.tratamientos.length()").value(2))
                    .andExpect(jsonPath("$.tratamientos[0].idTratamiento").value(endodoncia.getId().toString()))
                    .andExpect(jsonPath("$.tratamientos[0].nombre").value(endodoncia.getNombre()))
                    .andExpect(jsonPath("$.tratamientos[0].cantidad").value(2))
                    .andExpect(jsonPath("$.tratamientos[0].total").value(3300.00))
                    .andExpect(jsonPath("$.tratamientos[1].idTratamiento").value(limpieza.getId().toString()))
                    .andExpect(jsonPath("$.tratamientos[1].cantidad").value(2))
                    .andExpect(jsonPath("$.tratamientos[1].total").value(550.00));
        }

        ingresos(tokenAdmin, desde, desde)
                .andExpect(jsonPath("$.cantidadTotal").value(3))
                .andExpect(jsonPath("$.ingresoTotal").value(2350.00));
        ingresos(tokenAdmin, desde.minusDays(1), hasta)
                .andExpect(jsonPath("$.ingresoTotal").value(4849.00))
                .andExpect(jsonPath("$.tratamientos[1].cantidad").value(3));
        ingresos(tokenAdmin, hasta.plusYears(10), hasta.plusYears(10))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cantidadTotal").value(0))
                .andExpect(jsonPath("$.ingresoTotal").value(0))
                .andExpect(jsonPath("$.tratamientos").isEmpty());
    }

    @Test
    void doctorYPacienteNoVenLosReportes() throws Exception {
        for (String ruta : new String[] {INASISTENCIAS, INGRESOS}) {
            for (String token : new String[] {tokenDoctor, tokenCliente}) {
                mockMvc.perform(conToken(get(ruta).param("desde", desde.toString()).param("hasta", hasta.toString()), token))
                        .andExpect(status().isForbidden());
            }
            mockMvc.perform(get(ruta).param("desde", desde.toString()).param("hasta", hasta.toString()))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Test
    void fechasInvalidasDevuelven400() throws Exception {
        for (String ruta : new String[] {INASISTENCIAS, INGRESOS}) {
            mockMvc.perform(conToken(get(ruta).param("desde", desde.toString()), tokenAdmin))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.mensaje").value("Debes indicar las fechas desde y hasta"));
            mockMvc.perform(conToken(get(ruta).param("desde", "15/10/2026").param("hasta", hasta.toString()), tokenAdmin))
                    .andExpect(status().isBadRequest());
            mockMvc.perform(conToken(get(ruta).param("desde", hasta.toString()).param("hasta", desde.toString()), tokenAdmin))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.mensaje").value("La fecha desde no puede ser posterior a hasta"));
        }
    }

    // En el periodo: 2 NO_ASISTIO y 3 CANCELADA. Fuera del periodo: 1 de cada una. Las demas no cuentan.
    private void crearDatosDeInasistencias() {
        crearCita(doctorA, pacienteX, desde, "09:00", EstadoCita.NO_ASISTIO);
        crearCita(doctorA, pacienteX, desde.plusDays(1), "09:00", EstadoCita.CANCELADA);
        crearCita(doctorA, pacienteY, hasta, "09:00", EstadoCita.NO_ASISTIO);
        crearCita(doctorB, pacienteY, desde.plusDays(1), "10:00", EstadoCita.CANCELADA);
        crearCita(doctorB, pacienteY, desde.plusDays(1), "11:00", EstadoCita.CANCELADA);

        crearCita(doctorA, pacienteX, desde, "10:00", EstadoCita.AGENDADA);
        crearCita(doctorB, pacienteX, desde, "10:00", EstadoCita.ATENDIDA);

        crearCita(doctorA, pacienteX, desde.minusDays(1), "09:00", EstadoCita.NO_ASISTIO);
        crearCita(doctorB, pacienteY, hasta.plusDays(1), "09:00", EstadoCita.CANCELADA);
    }

    private Cita crearCita(Doctor doctor, Cliente paciente, LocalDate fecha, String hora, EstadoCita estado) {
        return citaRepository.save(Cita.builder()
                .cliente(paciente)
                .doctor(doctor)
                .fecha(fecha)
                .hora(LocalTime.parse(hora))
                .estado(estado)
                .build());
    }

    private Tratamiento crearTratamiento(String nombre, String costo) {
        return tratamientoRepository.save(Tratamiento.builder()
                .nombre(nombre + " " + UUID.randomUUID())
                .duracionMinutos(30)
                .costo(new BigDecimal(costo))
                .build());
    }

    private void registrar(Cita cita, Tratamiento tratamiento, String costo) {
        citaTratamientoRepository.save(CitaTratamiento.builder()
                .cita(cita)
                .tratamiento(tratamiento)
                .costo(new BigDecimal(costo))
                .fechaRegistro(LocalDateTime.now(clock))
                .build());
    }

    private ResultActions inasistencias(String token, LocalDate inicio, LocalDate fin) throws Exception {
        return mockMvc.perform(conToken(get(INASISTENCIAS).param("desde", inicio.toString()).param("hasta", fin.toString()), token));
    }

    private ResultActions ingresos(String token, LocalDate inicio, LocalDate fin) throws Exception {
        return mockMvc.perform(conToken(get(INGRESOS).param("desde", inicio.toString()).param("hasta", fin.toString()), token));
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
