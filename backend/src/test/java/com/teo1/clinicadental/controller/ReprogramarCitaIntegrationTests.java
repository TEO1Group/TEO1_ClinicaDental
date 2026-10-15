package com.teo1.clinicadental.controller;

import com.teo1.clinicadental.model.Cita;
import com.teo1.clinicadental.model.CitaReprogramacion;
import com.teo1.clinicadental.model.Cliente;
import com.teo1.clinicadental.model.DiaSemana;
import com.teo1.clinicadental.model.Doctor;
import com.teo1.clinicadental.model.EstadoCita;
import com.teo1.clinicadental.model.EstadoUsuario;
import com.teo1.clinicadental.model.Horario;
import com.teo1.clinicadental.model.Rol;
import com.teo1.clinicadental.model.Usuario;
import com.teo1.clinicadental.repository.CitaReprogramacionRepository;
import com.teo1.clinicadental.repository.CitaRepository;
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
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReprogramarCitaIntegrationTests {

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
    private CitaRepository citaRepository;

    @Autowired
    private CitaReprogramacionRepository citaReprogramacionRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private Clock clock;

    private String tokenSecretaria;
    private String tokenCliente;
    private String tokenOtroCliente;
    private String tokenDoctor;
    private Usuario usuarioCliente;
    private Doctor doctor;
    private LocalDate fecha;

    @BeforeEach
    void setUp() {
        tokenSecretaria = token(crearUsuario(Rol.SECRETARIA));

        usuarioCliente = crearUsuario(Rol.CLIENTE);
        clienteRepository.save(Cliente.builder().usuario(usuarioCliente).dpi(dpiAleatorio()).build());
        tokenCliente = token(usuarioCliente);

        Usuario usuarioOtroCliente = crearUsuario(Rol.CLIENTE);
        clienteRepository.save(Cliente.builder().usuario(usuarioOtroCliente).dpi(dpiAleatorio()).build());
        tokenOtroCliente = token(usuarioOtroCliente);

        Usuario usuarioDoctor = crearUsuario(Rol.DOCTOR);
        doctor = doctorRepository.save(Doctor.builder().usuario(usuarioDoctor).especialidad("Ortodoncia").build());
        tokenDoctor = token(usuarioDoctor);

        fecha = LocalDate.now(clock).plusDays(7);
        horarioRepository.save(Horario.builder()
                .doctor(doctor)
                .diaSemana(DiaSemana.desde(fecha.getDayOfWeek()))
                .horaInicio(LocalTime.of(8, 0))
                .horaFin(LocalTime.of(12, 0))
                .build());
    }

    @Test
    void clienteReprogramaSuCitaYQuedaRegistrada() throws Exception {
        UUID idCita = idCita(agendar(tokenCliente, "09:00").andExpect(status().isCreated()));
        Cita cita = citaRepository.findById(idCita).orElseThrow();
        cita.setRecordatorioEnviado(true);
        citaRepository.saveAndFlush(cita);

        reprogramar(idCita, tokenCliente, "10:00", "Cambio de turno en el trabajo")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idCita").value(idCita.toString()))
                .andExpect(jsonPath("$.fecha").value(fecha.toString()))
                .andExpect(jsonPath("$.estado").value("AGENDADA"))
                .andExpect(jsonPath("$.idDoctor").value(doctor.getId().toString()))
                .andExpect(jsonPath("$.motivo").value(nullValue()));

        Cita reprogramada = citaRepository.findById(idCita).orElseThrow();
        assertEquals(LocalTime.of(10, 0), reprogramada.getHora());
        assertFalse(reprogramada.isRecordatorioEnviado());

        List<CitaReprogramacion> registros = citaReprogramacionRepository.findByCitaIdOrderByFechaCambioAsc(idCita);
        assertEquals(1, registros.size());
        assertEquals(fecha, registros.get(0).getFechaAnterior());
        assertEquals(LocalTime.of(9, 0), registros.get(0).getHoraAnterior());
        assertEquals(fecha, registros.get(0).getFechaNueva());
        assertEquals(LocalTime.of(10, 0), registros.get(0).getHoraNueva());
        assertEquals("Cambio de turno en el trabajo", registros.get(0).getMotivo());
        assertEquals(usuarioCliente.getId(), registros.get(0).getReprogramadoPor().getId());
    }

    @Test
    void laHoraAnteriorQuedaLibre() throws Exception {
        UUID idCita = idCita(agendar(tokenCliente, "09:00").andExpect(status().isCreated()));

        reprogramar(idCita, tokenSecretaria, "11:00", "Pedido del paciente").andExpect(status().isOk());

        agendar(tokenOtroCliente, "09:00").andExpect(status().isCreated());
    }

    @Test
    void reprogramarSobreOtraCitaDevuelve409() throws Exception {
        UUID idCita = idCita(agendar(tokenCliente, "09:00").andExpect(status().isCreated()));
        agendar(tokenOtroCliente, "10:00").andExpect(status().isCreated());

        reprogramar(idCita, tokenCliente, "10:15", "Cambio")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("El doctor ya tiene una cita agendada en ese horario"));

        assertEquals(EstadoCita.AGENDADA, citaRepository.findById(idCita).orElseThrow().getEstado());
        assertEquals(0, citaReprogramacionRepository.findByCitaIdOrderByFechaCambioAsc(idCita).size());
    }

    @Test
    void doctorYClienteAjenoNoPuedenReprogramar() throws Exception {
        UUID idCita = idCita(agendar(tokenCliente, "09:00").andExpect(status().isCreated()));

        reprogramar(idCita, tokenDoctor, "10:00", "Cambio")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("Los doctores no pueden reprogramar citas"));
        reprogramar(idCita, tokenOtroCliente, "10:00", "Cambio").andExpect(status().isForbidden());
    }

    @Test
    void sinMotivoDevuelve400() throws Exception {
        UUID idCita = idCita(agendar(tokenCliente, "09:00").andExpect(status().isCreated()));

        mockMvc.perform(conToken(patch("/citas/" + idCita + "/reprogramar"), tokenCliente)
                        .content(String.format("{\"fecha\":\"%s\",\"hora\":\"10:00\"}", fecha)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("motivo: El motivo es obligatorio"));
    }

    private ResultActions agendar(String token, String hora) throws Exception {
        return mockMvc.perform(conToken(post("/citas"), token)
                .content(String.format("{\"idDoctor\":\"%s\",\"fecha\":\"%s\",\"hora\":\"%s\"}", doctor.getId(), fecha, hora)));
    }

    private ResultActions reprogramar(UUID idCita, String token, String hora, String motivo) throws Exception {
        return mockMvc.perform(conToken(patch("/citas/" + idCita + "/reprogramar"), token)
                .content(String.format("{\"fecha\":\"%s\",\"hora\":\"%s\",\"motivo\":\"%s\"}", fecha, hora, motivo)));
    }

    private UUID idCita(ResultActions resultado) throws Exception {
        String respuesta = resultado.andReturn().getResponse().getContentAsString();
        return UUID.fromString(respuesta.replaceAll(".*\"idCita\":\"([^\"]+)\".*", "$1"));
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
