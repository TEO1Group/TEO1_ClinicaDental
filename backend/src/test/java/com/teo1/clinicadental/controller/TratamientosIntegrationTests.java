package com.teo1.clinicadental.controller;

import com.teo1.clinicadental.model.Cita;
import com.teo1.clinicadental.model.Cliente;
import com.teo1.clinicadental.model.Doctor;
import com.teo1.clinicadental.model.EstadoCita;
import com.teo1.clinicadental.model.EstadoUsuario;
import com.teo1.clinicadental.model.Rol;
import com.teo1.clinicadental.model.Tratamiento;
import com.teo1.clinicadental.model.Usuario;
import com.teo1.clinicadental.repository.CitaRepository;
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
import java.time.LocalTime;
import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TratamientosIntegrationTests {

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
    private JwtService jwtService;

    @Autowired
    private Clock clock;

    private String tokenAdmin;
    private String tokenSecretaria;
    private String tokenDoctor;
    private String tokenOtroDoctor;
    private Cliente cliente;
    private Doctor doctor;
    private Tratamiento tratamiento;
    private Cita citaAtendida;

    @BeforeEach
    void setUp() {
        tokenAdmin = token(crearUsuario(Rol.ADMIN));
        tokenSecretaria = token(crearUsuario(Rol.SECRETARIA));

        cliente = clienteRepository.save(Cliente.builder().usuario(crearUsuario(Rol.CLIENTE)).dpi(dpiAleatorio()).build());

        Usuario usuarioDoctor = crearUsuario(Rol.DOCTOR);
        doctor = doctorRepository.save(Doctor.builder().usuario(usuarioDoctor).especialidad("Ortodoncia").build());
        tokenDoctor = token(usuarioDoctor);

        Usuario usuarioOtroDoctor = crearUsuario(Rol.DOCTOR);
        doctorRepository.save(Doctor.builder().usuario(usuarioOtroDoctor).especialidad("Endodoncia").build());
        tokenOtroDoctor = token(usuarioOtroDoctor);

        tratamiento = tratamientoRepository.save(Tratamiento.builder()
                .nombre("Tratamiento de prueba " + UUID.randomUUID())
                .descripcion("Solo para pruebas")
                .duracionMinutos(30)
                .costo(new BigDecimal("300.00"))
                .build());

        citaAtendida = crearCita(LocalDate.now(clock).minusDays(3), EstadoCita.ATENDIDA);
    }

    @Test
    void elDoctorDeLaCitaRegistraTratamientos() throws Exception {
        registrar(citaAtendida, tokenDoctor, String.format("{\"idTratamiento\":\"%s\"}", tratamiento.getId()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idCita").value(citaAtendida.getId().toString()))
                .andExpect(jsonPath("$.idTratamiento").value(tratamiento.getId().toString()))
                .andExpect(jsonPath("$.nombreTratamiento").value(tratamiento.getNombre()))
                .andExpect(jsonPath("$.costo").value(300.0))
                .andExpect(jsonPath("$.nombreDoctor").value("Prueba"))
                .andExpect(jsonPath("$.apellidoDoctor").value("DOCTOR"));

        registrar(citaAtendida, tokenDoctor, String.format(
                "{\"idTratamiento\":\"%s\",\"costo\":250.50,\"observaciones\":\"Pieza 36\"}", tratamiento.getId()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.costo").value(250.5))
                .andExpect(jsonPath("$.observaciones").value("Pieza 36"));

        mockMvc.perform(conToken(get("/citas/" + citaAtendida.getId() + "/tratamientos"), tokenDoctor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void costoNegativoYTratamientoInexistente() throws Exception {
        registrar(citaAtendida, tokenDoctor, String.format("{\"idTratamiento\":\"%s\",\"costo\":-1}", tratamiento.getId()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("costo: El costo no puede ser negativo"));

        registrar(citaAtendida, tokenDoctor, String.format("{\"idTratamiento\":\"%s\"}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Tratamiento no encontrado"));
    }

    @Test
    void citaAgendadaDevuelve409() throws Exception {
        Cita agendada = crearCita(LocalDate.now(clock).plusDays(3), EstadoCita.AGENDADA);

        registrar(agendada, tokenDoctor, String.format("{\"idTratamiento\":\"%s\"}", tratamiento.getId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value(
                        "Solo se pueden registrar tratamientos en una cita ATENDIDA y esta cita esta AGENDADA"));
    }

    @Test
    void otroDoctorYSecretariaNoRegistran() throws Exception {
        String cuerpo = String.format("{\"idTratamiento\":\"%s\"}", tratamiento.getId());

        registrar(citaAtendida, tokenOtroDoctor, cuerpo)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("Solo el doctor de la cita puede registrar tratamientos"));
        registrar(citaAtendida, tokenSecretaria, cuerpo).andExpect(status().isForbidden());

        mockMvc.perform(conToken(get("/citas/" + citaAtendida.getId() + "/tratamientos"), tokenOtroDoctor))
                .andExpect(status().isForbidden());
        mockMvc.perform(conToken(get("/citas/" + citaAtendida.getId() + "/tratamientos"), tokenSecretaria))
                .andExpect(status().isOk());
    }

    @Test
    void losTratamientosDelPacienteTraenElTotal() throws Exception {
        Cita otraAtendida = crearCita(LocalDate.now(clock).minusDays(5), EstadoCita.ATENDIDA);
        registrar(citaAtendida, tokenDoctor, String.format("{\"idTratamiento\":\"%s\"}", tratamiento.getId()))
                .andExpect(status().isCreated());
        registrar(otraAtendida, tokenDoctor, String.format("{\"idTratamiento\":\"%s\",\"costo\":150.25}", tratamiento.getId()))
                .andExpect(status().isCreated());

        mockMvc.perform(conToken(get("/pacientes/" + cliente.getId() + "/tratamientos"), tokenSecretaria))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(450.25))
                .andExpect(jsonPath("$.tratamientos", hasSize(2)))
                .andExpect(jsonPath("$.tratamientos[0].idCita").value(citaAtendida.getId().toString()))
                .andExpect(jsonPath("$.tratamientos[0].fechaCita").value(citaAtendida.getFecha().toString()))
                .andExpect(jsonPath("$.tratamientos[1].idCita").value(otraAtendida.getId().toString()));

        mockMvc.perform(conToken(get("/pacientes/" + cliente.getId() + "/tratamientos"), tokenOtroDoctor))
                .andExpect(status().isOk());
    }

    @Test
    void pacienteSinTratamientosOInexistente() throws Exception {
        mockMvc.perform(conToken(get("/pacientes/" + cliente.getId() + "/tratamientos"), tokenAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.tratamientos", hasSize(0)));

        mockMvc.perform(conToken(get("/pacientes/" + UUID.randomUUID() + "/tratamientos"), tokenAdmin))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Paciente no encontrado"));
    }

    @Test
    void elCatalogoResponde() throws Exception {
        for (String token : new String[] {tokenDoctor, tokenSecretaria, tokenAdmin}) {
            mockMvc.perform(conToken(get("/tratamientos"), token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[*].nombre", hasItem(tratamiento.getNombre())));
        }
    }

    private Cita crearCita(LocalDate fecha, EstadoCita estado) {
        return citaRepository.save(Cita.builder()
                .cliente(cliente)
                .doctor(doctor)
                .fecha(fecha)
                .hora(LocalTime.of(9, 0))
                .estado(estado)
                .build());
    }

    private ResultActions registrar(Cita cita, String token, String cuerpo) throws Exception {
        return mockMvc.perform(conToken(post("/citas/" + cita.getId() + "/tratamientos"), token).content(cuerpo));
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
