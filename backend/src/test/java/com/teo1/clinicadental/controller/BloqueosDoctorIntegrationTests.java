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

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BloqueosDoctorIntegrationTests {

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
    private String tokenDoctor;
    private String tokenOtroDoctor;
    private Usuario usuarioSecretaria;
    private Doctor doctor;
    private LocalDate fecha;

    @BeforeEach
    void setUp() {
        tokenAdmin = token(crearUsuario(Rol.ADMIN));
        usuarioSecretaria = crearUsuario(Rol.SECRETARIA);
        tokenSecretaria = token(usuarioSecretaria);

        Usuario usuarioCliente = crearUsuario(Rol.CLIENTE);
        clienteRepository.save(Cliente.builder().usuario(usuarioCliente).dpi(dpiAleatorio()).build());
        tokenCliente = token(usuarioCliente);

        Usuario usuarioDoctor = crearUsuario(Rol.DOCTOR);
        doctor = doctorRepository.save(Doctor.builder().usuario(usuarioDoctor).especialidad("Ortodoncia").build());
        tokenDoctor = token(usuarioDoctor);

        Usuario usuarioOtroDoctor = crearUsuario(Rol.DOCTOR);
        doctorRepository.save(Doctor.builder().usuario(usuarioOtroDoctor).especialidad("Endodoncia").build());
        tokenOtroDoctor = token(usuarioOtroDoctor);

        fecha = LocalDate.now(clock).plusDays(7);
        horarioRepository.save(Horario.builder()
                .doctor(doctor)
                .diaSemana(DiaSemana.desde(fecha.getDayOfWeek()))
                .horaInicio(LocalTime.of(8, 0))
                .horaFin(LocalTime.of(12, 0))
                .build());
    }

    @Test
    void secretariaYAdminCreanBloqueos() throws Exception {
        crearBloqueo(tokenSecretaria, "08:00", "09:00", "Capacitacion")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idDoctor").value(doctor.getId().toString()))
                .andExpect(jsonPath("$.inicio").value(fecha + "T08:00:00"))
                .andExpect(jsonPath("$.fin").value(fecha + "T09:00:00"))
                .andExpect(jsonPath("$.motivo").value("Capacitacion"))
                .andExpect(jsonPath("$.idCreadoPor").value(usuarioSecretaria.getId().toString()));

        crearBloqueo(tokenAdmin, "10:00", "11:00", "Congreso de ortodoncia").andExpect(status().isCreated());
    }

    @Test
    void clienteYDoctorNoCreanBloqueos() throws Exception {
        crearBloqueo(tokenCliente, "10:00", "11:00", "Congreso").andExpect(status().isForbidden());
        crearBloqueo(tokenDoctor, "10:00", "11:00", "Congreso").andExpect(status().isForbidden());
    }

    @Test
    void datosInvalidosDevuelven400() throws Exception {
        crearBloqueo(tokenAdmin, "11:00", "10:00", "Congreso")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("El inicio del bloqueo debe ser anterior al fin"));
        crearBloqueo(tokenAdmin, "10:00", "11:00", "  ").andExpect(status().isBadRequest());

        mockMvc.perform(conToken(post("/doctores/" + UUID.randomUUID() + "/bloqueos"), tokenAdmin)
                        .content(cuerpo("10:00", "11:00", "Congreso")))
                .andExpect(status().isNotFound());
    }

    @Test
    void citaAgendadaEnElPeriodoImpideCrearElBloqueo() throws Exception {
        String idCita = idDe(agendar("10:30").andExpect(status().isCreated()));

        crearBloqueo(tokenSecretaria, "10:00", "11:00", "Congreso")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value(
                        "El doctor tiene 1 cita agendada en ese periodo; primero deben reprogramarse o cancelarse"));

        mockMvc.perform(conToken(patch("/citas/" + idCita + "/estado"), tokenSecretaria)
                        .content("{\"estado\":\"CANCELADA\",\"motivo\":\"El doctor no estara\"}"))
                .andExpect(status().isOk());

        crearBloqueo(tokenSecretaria, "10:00", "11:00", "Congreso").andExpect(status().isCreated());
    }

    @Test
    void agendarDentroDelBloqueoDevuelve409ConElMotivoYLosBordesSeAceptan() throws Exception {
        crearBloqueo(tokenAdmin, "10:00", "11:00", "Congreso de ortodoncia").andExpect(status().isCreated());

        agendar("10:00")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("El doctor no atiende en ese horario: Congreso de ortodoncia"));
        agendar("09:45").andExpect(status().isConflict());

        agendar("09:30").andExpect(status().isCreated());
        agendar("11:00").andExpect(status().isCreated());
    }

    @Test
    void trasEliminarElBloqueoSePuedeAgendar() throws Exception {
        String idBloqueo = idDe(crearBloqueo(tokenAdmin, "10:00", "11:00", "Congreso").andExpect(status().isCreated()));
        agendar("10:00").andExpect(status().isConflict());

        mockMvc.perform(conToken(delete("/doctores/" + doctor.getId() + "/bloqueos/" + idBloqueo), tokenSecretaria))
                .andExpect(status().isNoContent());
        mockMvc.perform(conToken(delete("/doctores/" + doctor.getId() + "/bloqueos/" + idBloqueo), tokenSecretaria))
                .andExpect(status().isNotFound());

        agendar("10:00").andExpect(status().isCreated());
    }

    @Test
    void listarBloqueosSegunElRol() throws Exception {
        crearBloqueo(tokenAdmin, "10:00", "11:00", "Congreso").andExpect(status().isCreated());
        crearBloqueo(tokenAdmin, "08:00", "09:00", "Capacitacion").andExpect(status().isCreated());
        String ruta = "/doctores/" + doctor.getId() + "/bloqueos";

        mockMvc.perform(conToken(get(ruta), tokenDoctor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].motivo").value("Capacitacion"))
                .andExpect(jsonPath("$[1].motivo").value("Congreso"));
        mockMvc.perform(conToken(get(ruta), tokenSecretaria)).andExpect(status().isOk());
        mockMvc.perform(conToken(get(ruta), tokenAdmin)).andExpect(status().isOk());

        mockMvc.perform(conToken(get(ruta), tokenOtroDoctor)).andExpect(status().isForbidden());
        mockMvc.perform(conToken(get(ruta), tokenCliente)).andExpect(status().isForbidden());
    }

    private ResultActions crearBloqueo(String token, String inicio, String fin, String motivo) throws Exception {
        return mockMvc.perform(conToken(post("/doctores/" + doctor.getId() + "/bloqueos"), token)
                .content(cuerpo(inicio, fin, motivo)));
    }

    private String cuerpo(String inicio, String fin, String motivo) {
        return String.format("{\"inicio\":\"%sT%s:00\",\"fin\":\"%sT%s:00\",\"motivo\":\"%s\"}",
                fecha, inicio, fecha, fin, motivo);
    }

    private ResultActions agendar(String hora) throws Exception {
        return mockMvc.perform(conToken(post("/citas"), tokenCliente)
                .content(String.format("{\"idDoctor\":\"%s\",\"fecha\":\"%s\",\"hora\":\"%s\"}", doctor.getId(), fecha, hora)));
    }

    private String idDe(ResultActions resultado) throws Exception {
        String respuesta = resultado.andReturn().getResponse().getContentAsString();
        return respuesta.replaceAll(".*\"id(?:Cita|Bloqueo)\":\"([^\"]+)\".*", "$1");
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
