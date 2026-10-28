package com.teo1.clinicadental.controller;

import com.teo1.clinicadental.model.Cita;
import com.teo1.clinicadental.model.Cliente;
import com.teo1.clinicadental.model.DiaSemana;
import com.teo1.clinicadental.model.Doctor;
import com.teo1.clinicadental.model.EstadoCita;
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

import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class InasistenciasIntegrationTests {

    private static final String MENSAJE_BLOQUEO = "El paciente tiene 3 inasistencias y no puede agendar citas "
            + "hasta que secretaria o administracion le levanten la restriccion";

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
    private JwtService jwtService;

    @Autowired
    private Clock clock;

    private String tokenAdmin;
    private String tokenSecretaria;
    private String tokenDoctor;
    private String tokenCliente;
    private Usuario usuarioSecretaria;
    private Cliente cliente;
    private Doctor doctor;
    private LocalDate fecha;

    @BeforeEach
    void setUp() {
        tokenAdmin = token(crearUsuario(Rol.ADMIN));
        usuarioSecretaria = crearUsuario(Rol.SECRETARIA);
        tokenSecretaria = token(usuarioSecretaria);

        Usuario usuarioCliente = crearUsuario(Rol.CLIENTE);
        cliente = clienteRepository.save(Cliente.builder().usuario(usuarioCliente).dpi(dpiAleatorio()).build());
        tokenCliente = token(usuarioCliente);

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
    void conDosInasistenciasSigueAgendandoYLasCanceladasNoCuentan() throws Exception {
        inasistenciasPasadas(2);
        guardarCita(LocalDate.now(clock).minusDays(5), LocalTime.of(9, 0), EstadoCita.CANCELADA);

        estado(tokenSecretaria)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idCliente").value(cliente.getId().toString()))
                .andExpect(jsonPath("$.inasistencias").value(2))
                .andExpect(jsonPath("$.limite").value(3))
                .andExpect(jsonPath("$.bloqueado").value(false))
                .andExpect(jsonPath("$.ultimoLevantamiento").value(nullValue()));

        agendar(tokenCliente, "09:00").andExpect(status().isCreated());
    }

    @Test
    void conTresInasistenciasNoAgendaNiElPacienteNiLaSecretaria() throws Exception {
        inasistenciasPasadas(3);

        estado(tokenDoctor)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inasistencias").value(3))
                .andExpect(jsonPath("$.bloqueado").value(true));

        agendar(tokenCliente, "09:00")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value(MENSAJE_BLOQUEO));
        agendar(tokenSecretaria, "09:00")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value(MENSAJE_BLOQUEO));
    }

    @Test
    void alLevantarlaElConteoEmpiezaDeCeroYQuedaQuienCuandoYPorQue() throws Exception {
        inasistenciasPasadas(3);

        levantar(tokenSecretaria, "  Presento constancia medica  ")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.inasistencias").value(0))
                .andExpect(jsonPath("$.bloqueado").value(false))
                .andExpect(jsonPath("$.ultimoLevantamiento.motivo").value("Presento constancia medica"))
                .andExpect(jsonPath("$.ultimoLevantamiento.inasistencias").value(3))
                .andExpect(jsonPath("$.ultimoLevantamiento.idLevantadoPor").value(usuarioSecretaria.getId().toString()))
                .andExpect(jsonPath("$.ultimoLevantamiento.fecha").exists());

        estado(tokenAdmin)
                .andExpect(jsonPath("$.inasistencias").value(0))
                .andExpect(jsonPath("$.ultimoLevantamiento.motivo").value("Presento constancia medica"));
        agendar(tokenCliente, "09:00").andExpect(status().isCreated());
    }

    @Test
    void trasLevantarlaTresInasistenciasNuevasLaVuelvenAActivar() throws Exception {
        inasistenciasPasadas(3);
        levantar(tokenAdmin, "Se comprometio a avisar").andExpect(status().isCreated());

        // Citas posteriores al levantamiento que el paciente no atendio
        for (int i = 0; i < 3; i++) {
            guardarCita(fecha.plusDays(1), LocalTime.of(8 + i, 0), EstadoCita.NO_ASISTIO);
        }

        estado(tokenSecretaria)
                .andExpect(jsonPath("$.inasistencias").value(3))
                .andExpect(jsonPath("$.bloqueado").value(true));
        agendar(tokenCliente, "09:00").andExpect(status().isConflict());
    }

    @Test
    void levantarSinRestriccionOConDatosInvalidosSeRechaza() throws Exception {
        inasistenciasPasadas(2);
        levantar(tokenSecretaria, "Sin motivo real")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("El paciente no tiene la restriccion por inasistencias (2 de 3)"));

        inasistenciasPasadas(1);
        levantar(tokenSecretaria, "   ").andExpect(status().isBadRequest());
        levantar(tokenSecretaria, "x".repeat(201)).andExpect(status().isBadRequest());

        mockMvc.perform(conToken(post("/pacientes/" + UUID.randomUUID() + "/inasistencias/levantamientos"), tokenSecretaria)
                        .content("{\"motivo\":\"Constancia\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(conToken(get("/pacientes/" + UUID.randomUUID() + "/inasistencias"), tokenSecretaria))
                .andExpect(status().isNotFound());
    }

    @Test
    void soloAdminYSecretariaLevantanYElPacienteNoConsulta() throws Exception {
        inasistenciasPasadas(3);

        levantar(tokenDoctor, "Constancia").andExpect(status().isForbidden());
        levantar(tokenCliente, "Constancia").andExpect(status().isForbidden());
        estado(tokenCliente).andExpect(status().isForbidden());
        mockMvc.perform(get("/pacientes/" + cliente.getId() + "/inasistencias")).andExpect(status().isUnauthorized());
    }

    private void inasistenciasPasadas(int cantidad) {
        LocalDate dia = LocalDate.now(clock).minusDays(30 + citaRepository.countByClienteIdAndEstado(cliente.getId(), EstadoCita.NO_ASISTIO));
        for (int i = 0; i < cantidad; i++) {
            guardarCita(dia.minusDays(i), LocalTime.of(10, 0), EstadoCita.NO_ASISTIO);
        }
    }

    private void guardarCita(LocalDate dia, LocalTime hora, EstadoCita estado) {
        citaRepository.save(Cita.builder()
                .cliente(cliente)
                .doctor(doctor)
                .fecha(dia)
                .hora(hora)
                .estado(estado)
                .recordatorioEnviado(false)
                .build());
    }

    private ResultActions estado(String token) throws Exception {
        return mockMvc.perform(conToken(get("/pacientes/" + cliente.getId() + "/inasistencias"), token));
    }

    private ResultActions levantar(String token, String motivo) throws Exception {
        return mockMvc.perform(conToken(post("/pacientes/" + cliente.getId() + "/inasistencias/levantamientos"), token)
                .content("{\"motivo\":\"" + motivo + "\"}"));
    }

    private ResultActions agendar(String token, String hora) throws Exception {
        return mockMvc.perform(conToken(post("/citas"), token)
                .content(String.format("{\"idDoctor\":\"%s\",\"idCliente\":\"%s\",\"fecha\":\"%s\",\"hora\":\"%s\"}",
                        doctor.getId(), cliente.getId(), fecha, hora)));
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
