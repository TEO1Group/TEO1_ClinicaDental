package com.teo1.clinicadental.controller;

import com.teo1.clinicadental.model.Cita;
import com.teo1.clinicadental.model.Cliente;
import com.teo1.clinicadental.model.Doctor;
import com.teo1.clinicadental.model.EstadoCita;
import com.teo1.clinicadental.model.EstadoUsuario;
import com.teo1.clinicadental.model.Rol;
import com.teo1.clinicadental.model.Usuario;
import com.teo1.clinicadental.repository.CitaRepository;
import com.teo1.clinicadental.repository.ClienteRepository;
import com.teo1.clinicadental.repository.DoctorRepository;
import com.teo1.clinicadental.repository.RolRepository;
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
import org.springframework.test.web.servlet.result.JsonPathResultMatchers;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReporteCitasIntegrationTests {

    private static final String RUTA = "/reportes/citas-por-doctor";

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
    private JwtService jwtService;

    @Autowired
    private Clock clock;

    private String tokenAdmin;
    private String tokenSecretaria;
    private String tokenDoctor;
    private String tokenCliente;
    private Cliente cliente;
    private Doctor doctorConCitas;
    private Doctor otroDoctor;
    private Doctor doctorSinCitas;
    private Doctor doctorInactivo;
    private LocalDate desde;
    private LocalDate hasta;

    @BeforeEach
    void setUp() {
        tokenAdmin = token(crearUsuario(Rol.ADMIN, EstadoUsuario.ACTIVO));
        tokenSecretaria = token(crearUsuario(Rol.SECRETARIA, EstadoUsuario.ACTIVO));

        Usuario usuarioCliente = crearUsuario(Rol.CLIENTE, EstadoUsuario.ACTIVO);
        cliente = clienteRepository.save(Cliente.builder().usuario(usuarioCliente).dpi(dpiAleatorio()).build());
        tokenCliente = token(usuarioCliente);

        Usuario usuarioDoctor = crearUsuario(Rol.DOCTOR, EstadoUsuario.ACTIVO);
        doctorConCitas = doctorRepository.save(Doctor.builder().usuario(usuarioDoctor).especialidad("Ortodoncia").build());
        tokenDoctor = token(usuarioDoctor);
        otroDoctor = doctorRepository.save(Doctor.builder()
                .usuario(crearUsuario(Rol.DOCTOR, EstadoUsuario.ACTIVO)).especialidad("Endodoncia").build());
        doctorSinCitas = doctorRepository.save(Doctor.builder()
                .usuario(crearUsuario(Rol.DOCTOR, EstadoUsuario.ACTIVO)).especialidad("Periodoncia").build());
        doctorInactivo = doctorRepository.save(Doctor.builder()
                .usuario(crearUsuario(Rol.DOCTOR, EstadoUsuario.INACTIVO)).especialidad("Cirugia").build());

        desde = LocalDate.now(clock).plusDays(30);
        hasta = desde.plusDays(2);

        crearCita(doctorConCitas, desde, "09:00", EstadoCita.AGENDADA);
        crearCita(doctorConCitas, desde.plusDays(1), "09:00", EstadoCita.ATENDIDA);
        crearCita(doctorConCitas, desde.plusDays(1), "10:00", EstadoCita.CANCELADA);
        crearCita(doctorConCitas, desde.plusDays(1), "11:00", EstadoCita.ATENDIDA);
        crearCita(doctorConCitas, hasta, "09:00", EstadoCita.NO_ASISTIO);
        crearCita(doctorConCitas, desde.minusDays(1), "09:00", EstadoCita.ATENDIDA);
        crearCita(doctorConCitas, hasta.plusDays(1), "09:00", EstadoCita.AGENDADA);

        crearCita(otroDoctor, desde.plusDays(1), "09:00", EstadoCita.CANCELADA);
        crearCita(otroDoctor, desde.plusDays(1), "10:00", EstadoCita.CANCELADA);

        crearCita(doctorInactivo, desde.plusDays(1), "09:00", EstadoCita.ATENDIDA);
    }

    @Test
    void adminYSecretariaVenLosConteos() throws Exception {
        for (String token : new String[] {tokenAdmin, tokenSecretaria}) {
            reporte(token, desde.toString(), hasta.toString())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.desde").value(desde.toString()))
                    .andExpect(jsonPath("$.hasta").value(hasta.toString()))
                    .andExpect(campo(doctorConCitas, "agendadas").value(contains(1)))
                    .andExpect(campo(doctorConCitas, "atendidas").value(contains(2)))
                    .andExpect(campo(doctorConCitas, "canceladas").value(contains(1)))
                    .andExpect(campo(doctorConCitas, "noAsistio").value(contains(1)))
                    .andExpect(campo(doctorConCitas, "total").value(contains(5)))
                    .andExpect(campo(doctorConCitas, "especialidad").value(contains("Ortodoncia")))
                    .andExpect(campo(otroDoctor, "canceladas").value(contains(2)))
                    .andExpect(campo(otroDoctor, "total").value(contains(2)))
                    .andExpect(campo(doctorSinCitas, "total").value(contains(0)))
                    .andExpect(campo(doctorSinCitas, "agendadas").value(contains(0)))
                    .andExpect(campo(doctorInactivo, "total").value(empty()));
        }
    }

    @Test
    void losBordesDelPeriodoCuentanYLoDeFueraNo() throws Exception {
        reporte(tokenAdmin, desde.toString(), desde.toString())
                .andExpect(status().isOk())
                .andExpect(campo(doctorConCitas, "agendadas").value(contains(1)))
                .andExpect(campo(doctorConCitas, "total").value(contains(1)));

        reporte(tokenAdmin, hasta.toString(), hasta.toString())
                .andExpect(status().isOk())
                .andExpect(campo(doctorConCitas, "noAsistio").value(contains(1)))
                .andExpect(campo(doctorConCitas, "total").value(contains(1)));

        reporte(tokenAdmin, desde.minusDays(1).toString(), hasta.plusDays(1).toString())
                .andExpect(status().isOk())
                .andExpect(campo(doctorConCitas, "total").value(contains(7)));
    }

    @Test
    void parametrosInvalidosDevuelven400() throws Exception {
        mockMvc.perform(conToken(get(RUTA).param("desde", desde.toString()), tokenAdmin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Debes indicar las fechas desde y hasta"));
        mockMvc.perform(conToken(get(RUTA), tokenAdmin))
                .andExpect(status().isBadRequest());

        reporte(tokenAdmin, "15/10/2026", hasta.toString())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Valor invalido para desde"));

        reporte(tokenAdmin, hasta.toString(), desde.toString())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("La fecha desde no puede ser posterior a hasta"));
    }

    @Test
    void doctorYClienteNoVenElReporte() throws Exception {
        reporte(tokenDoctor, desde.toString(), hasta.toString()).andExpect(status().isForbidden());
        reporte(tokenCliente, desde.toString(), hasta.toString()).andExpect(status().isForbidden());
    }

    @Test
    void horasAgrupaMinutosEstadosYDoctoresEnUnaHora() throws Exception {
        crearCita(doctorConCitas, desde, "09:30", EstadoCita.CANCELADA);
        for (String token : new String[] {tokenAdmin, tokenSecretaria}) {
            horas(token, desde.toString(), hasta.toString())
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.desde").value(desde.toString()))
                    .andExpect(jsonPath("$.hasta").value(hasta.toString()))
                    .andExpect(jsonPath("$.horas.length()").value(3))
                    .andExpect(jsonPath("$.horas[0].hora").value(9))
                    .andExpect(jsonPath("$.horas[0].cantidad").value(6))
                    .andExpect(jsonPath("$.horas[1].hora").value(10))
                    .andExpect(jsonPath("$.horas[1].cantidad").value(2))
                    .andExpect(jsonPath("$.horas[2].hora").value(11))
                    .andExpect(jsonPath("$.horas[2].cantidad").value(1));
        }
    }

    @Test
    void horasRespetaAmbosLimitesYDevuelveVacioSinDatos() throws Exception {
        horas(tokenAdmin, desde.toString(), desde.toString())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.horas.length()").value(1))
                .andExpect(jsonPath("$.horas[0].cantidad").value(1));
        horas(tokenAdmin, hasta.toString(), hasta.toString())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.horas[0].cantidad").value(1));
        horas(tokenAdmin, hasta.plusYears(10).toString(), hasta.plusYears(10).toString())
                .andExpect(status().isOk()).andExpect(jsonPath("$.horas").isEmpty());
    }

    @Test
    void horasValidaFechasYPermisos() throws Exception {
        mockMvc.perform(conToken(get("/reportes/horas-demandadas").param("desde", desde.toString()), tokenAdmin))
                .andExpect(status().isBadRequest());
        mockMvc.perform(conToken(get("/reportes/horas-demandadas").param("hasta", hasta.toString()), tokenAdmin))
                .andExpect(status().isBadRequest());
        horas(tokenAdmin, "invalid", hasta.toString()).andExpect(status().isBadRequest());
        horas(tokenAdmin, hasta.toString(), desde.toString()).andExpect(status().isBadRequest());
        horas(tokenDoctor, desde.toString(), hasta.toString()).andExpect(status().isForbidden());
        horas(tokenCliente, desde.toString(), hasta.toString()).andExpect(status().isForbidden());
        mockMvc.perform(get("/reportes/horas-demandadas").param("desde", desde.toString()).param("hasta", hasta.toString()))
                .andExpect(status().isUnauthorized());
    }

    private ResultActions horas(String token, String inicio, String fin) throws Exception {
        return mockMvc.perform(conToken(get("/reportes/horas-demandadas").param("desde", inicio).param("hasta", fin), token));
    }

    private ResultActions reporte(String token, String desdeTexto, String hastaTexto) throws Exception {
        return mockMvc.perform(conToken(get(RUTA).param("desde", desdeTexto).param("hasta", hastaTexto), token));
    }

    private JsonPathResultMatchers campo(Doctor doctor, String nombre) {
        return jsonPath("$.doctores[?(@.idDoctor == '" + doctor.getId() + "')]." + nombre);
    }

    private void crearCita(Doctor doctor, LocalDate fecha, String hora, EstadoCita estado) {
        citaRepository.save(Cita.builder()
                .cliente(cliente)
                .doctor(doctor)
                .fecha(fecha)
                .hora(LocalTime.parse(hora))
                .estado(estado)
                .build());
    }

    private Usuario crearUsuario(Rol rol, EstadoUsuario estado) {
        return usuarioRepository.save(Usuario.builder()
                .nombre("Prueba")
                .apellido(rol.name())
                .email(rol.name().toLowerCase() + "-" + UUID.randomUUID() + "@prueba.com")
                .passwordHash("sin-uso")
                .rol(rolRepository.findByNombreRol(rol).orElseThrow())
                .estado(estado)
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
