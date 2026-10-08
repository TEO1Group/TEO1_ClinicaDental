package com.teo1.clinicadental.controller;

import com.teo1.clinicadental.model.Cliente;
import com.teo1.clinicadental.model.DiaSemana;
import com.teo1.clinicadental.model.Doctor;
import com.teo1.clinicadental.model.EstadoUsuario;
import com.teo1.clinicadental.model.Horario;
import com.teo1.clinicadental.model.Rol;
import com.teo1.clinicadental.model.Usuario;
import com.teo1.clinicadental.repository.ClienteRepository;
import com.teo1.clinicadental.repository.DoctorRepository;
import com.teo1.clinicadental.repository.HorarioRepository;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CitasIntegrationTests {

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
    private HorarioRepository horarioRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private Clock clock;

    private String tokenAdmin;
    private String tokenSecretaria;
    private String tokenCliente;
    private String tokenOtroCliente;
    private String tokenDoctor;
    private String tokenOtroDoctor;
    private Cliente cliente;
    private Cliente otroCliente;
    private Doctor doctor;
    private LocalDate fecha;

    @BeforeEach
    void setUp() {
        tokenAdmin = token(crearUsuario(Rol.ADMIN));
        tokenSecretaria = token(crearUsuario(Rol.SECRETARIA));

        Usuario usuarioCliente = crearUsuario(Rol.CLIENTE);
        cliente = clienteRepository.save(Cliente.builder().usuario(usuarioCliente).dpi(dpiAleatorio()).build());
        tokenCliente = token(usuarioCliente);

        Usuario usuarioOtroCliente = crearUsuario(Rol.CLIENTE);
        otroCliente = clienteRepository.save(Cliente.builder().usuario(usuarioOtroCliente).dpi(dpiAleatorio()).build());
        tokenOtroCliente = token(usuarioOtroCliente);

        Usuario usuarioDoctor = crearUsuario(Rol.DOCTOR);
        doctor = doctorRepository.save(Doctor.builder().usuario(usuarioDoctor).especialidad("Ortodoncia").build());
        tokenDoctor = token(usuarioDoctor);

        Usuario usuarioOtroDoctor = crearUsuario(Rol.DOCTOR);
        doctorRepository.save(Doctor.builder().usuario(usuarioOtroDoctor).especialidad("Endodoncia").build());
        tokenOtroDoctor = token(usuarioOtroDoctor);

        // Fecha futura calculada al correr, con horario de 08:00 a 12:00 ese dia de la semana,
        // para que las pruebas no dependan del dia en que corre el CI
        fecha = LocalDate.now(clock).plusDays(7);
        horarioRepository.save(Horario.builder()
                .doctor(doctor)
                .diaSemana(DiaSemana.desde(fecha.getDayOfWeek()))
                .horaInicio(LocalTime.of(8, 0))
                .horaFin(LocalTime.of(12, 0))
                .build());
    }

    @Test
    void citaValidaQuedaAgendada() throws Exception {
        agendar(tokenCliente, fecha, "09:00", null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("AGENDADA"))
                .andExpect(jsonPath("$.idDoctor").value(doctor.getId().toString()));
    }

    @Test
    void mismaHoraYMismoDoctorDevuelve409() throws Exception {
        agendar(tokenCliente, fecha, "09:00", null).andExpect(status().isCreated());

        agendar(tokenOtroCliente, fecha, "09:00", null).andExpect(status().isConflict());
    }

    @Test
    void traslapeConOtraHoraDeInicioDevuelve409() throws Exception {
        agendar(tokenCliente, fecha, "09:00", null).andExpect(status().isCreated());

        agendar(tokenOtroCliente, fecha, "09:15", null).andExpect(status().isConflict());
    }

    @Test
    void citaQueEmpiezaCuandoTerminaOtraSeAcepta() throws Exception {
        agendar(tokenCliente, fecha, "09:00", null).andExpect(status().isCreated());

        agendar(tokenOtroCliente, fecha, "09:30", null).andExpect(status().isCreated());
    }

    @Test
    void citaFueraDelHorarioDevuelve409() throws Exception {
        agendar(tokenCliente, fecha, "07:45", null).andExpect(status().isConflict());
        agendar(tokenCliente, fecha, "11:45", null).andExpect(status().isConflict());
        agendar(tokenCliente, fecha.plusDays(1), "09:00", null).andExpect(status().isConflict());
    }

    @Test
    void trasCancelarLaHoraSePuedeVolverAAgendar() throws Exception {
        String idCita = idCita(agendar(tokenCliente, fecha, "09:00", null).andExpect(status().isCreated()));

        cambiarEstado(idCita, "CANCELADA", tokenCliente)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CANCELADA"));

        agendar(tokenOtroCliente, fecha, "09:00", null).andExpect(status().isCreated());
    }

    @Test
    void pacienteEnListaNegraDevuelve403() throws Exception {
        cliente.setEnListaNegra(true);
        clienteRepository.save(cliente);

        agendar(tokenCliente, fecha, "09:00", null).andExpect(status().isForbidden());
    }

    @Test
    void fechaPasadaDevuelve400() throws Exception {
        agendar(tokenCliente, LocalDate.now(clock).minusDays(1), "09:00", null).andExpect(status().isBadRequest());
    }

    @Test
    void quienPuedeAgendarYParaQuien() throws Exception {
        // El CLIENTE agenda para si mismo aunque mande otro idCliente
        agendar(tokenCliente, fecha, "09:00", otroCliente.getId())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idCliente").value(cliente.getId().toString()));

        agendar(tokenSecretaria, fecha, "10:00", otroCliente.getId())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idCliente").value(otroCliente.getId().toString()));

        agendar(tokenDoctor, fecha, "11:00", cliente.getId()).andExpect(status().isForbidden());
    }

    @Test
    void transicionesDeEstado() throws Exception {
        String idCita = idCita(agendar(tokenCliente, fecha, "09:00", null).andExpect(status().isCreated()));

        cambiarEstado(idCita, "ATENDIDA", tokenOtroDoctor).andExpect(status().isForbidden());

        cambiarEstado(idCita, "ATENDIDA", tokenDoctor)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ATENDIDA"));

        cambiarEstado(idCita, "CANCELADA", tokenAdmin).andExpect(status().isConflict());
        cambiarEstado(idCita, "AGENDADA", tokenAdmin).andExpect(status().isBadRequest());
    }

    @Test
    void cadaRolVeSoloLasCitasQueLeCorresponden() throws Exception {
        String citaPropia = idCita(agendar(tokenCliente, fecha, "09:00", null).andExpect(status().isCreated()));
        String citaAjena = idCita(agendar(tokenOtroCliente, fecha, "10:00", null).andExpect(status().isCreated()));

        mockMvc.perform(conToken(get("/citas"), tokenCliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].idCita", hasItem(citaPropia)))
                .andExpect(jsonPath("$[*].idCita", not(hasItem(citaAjena))));

        mockMvc.perform(conToken(get("/citas"), tokenSecretaria).param("idDoctor", doctor.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].idCita", hasItem(citaPropia)))
                .andExpect(jsonPath("$[*].idCita", hasItem(citaAjena)));

        mockMvc.perform(conToken(get("/citas"), tokenOtroDoctor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].idCita", not(hasItem(citaPropia))));

        mockMvc.perform(conToken(get("/citas/" + citaAjena), tokenCliente))
                .andExpect(status().isForbidden());

        mockMvc.perform(conToken(get("/citas/" + citaPropia), tokenDoctor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idCita").value(citaPropia));
    }

    private ResultActions agendar(String token, LocalDate dia, String hora, UUID idCliente) throws Exception {
        String body = idCliente == null
                ? String.format("{\"idDoctor\":\"%s\",\"fecha\":\"%s\",\"hora\":\"%s\"}", doctor.getId(), dia, hora)
                : String.format("{\"idDoctor\":\"%s\",\"idCliente\":\"%s\",\"fecha\":\"%s\",\"hora\":\"%s\"}",
                        doctor.getId(), idCliente, dia, hora);
        return mockMvc.perform(conToken(post("/citas"), token).content(body));
    }

    private ResultActions cambiarEstado(String idCita, String estado, String token) throws Exception {
        return mockMvc.perform(conToken(patch("/citas/" + idCita + "/estado"), token)
                .content("{\"estado\":\"" + estado + "\"}"));
    }

    private String idCita(ResultActions resultado) throws Exception {
        String respuesta = resultado.andReturn().getResponse().getContentAsString();
        return respuesta.replaceAll(".*\"idCita\":\"([^\"]+)\".*", "$1");
    }

    private Usuario crearUsuario(Rol rol) {
        return usuarioRepository.save(Usuario.builder()
                .nombre("Prueba")
                .apellido(rol.name())
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
