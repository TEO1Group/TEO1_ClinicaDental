package com.teo1.clinicadental.controller;

import com.teo1.clinicadental.model.BloqueoDoctor;
import com.teo1.clinicadental.model.Doctor;
import com.teo1.clinicadental.model.EstadoUsuario;
import com.teo1.clinicadental.model.Rol;
import com.teo1.clinicadental.model.Usuario;
import com.teo1.clinicadental.repository.BloqueoDoctorRepository;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BloqueosVigentesIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private BloqueoDoctorRepository bloqueoDoctorRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private Clock clock;

    private String tokenCliente;
    private String tokenAdmin;
    private String tokenDoctor;
    private Doctor doctor;
    private LocalDateTime ahora;

    @BeforeEach
    void setUp() {
        tokenCliente = token(crearUsuario(Rol.CLIENTE));
        tokenAdmin = token(crearUsuario(Rol.ADMIN));

        Usuario usuarioDoctor = crearUsuario(Rol.DOCTOR);
        doctor = doctorRepository.save(Doctor.builder().usuario(usuarioDoctor).especialidad("Ortodoncia").build());
        tokenDoctor = token(usuarioDoctor);

        ahora = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES);
    }

    @Test
    void elClienteVeElBloqueoEnCursoConSuMotivoYNoElPasado() throws Exception {
        crearBloqueo(ahora.minusDays(3), ahora.minusDays(2), "Vacaciones pasadas");
        crearBloqueo(ahora.plusDays(2), ahora.plusDays(2).plusHours(4), "Congreso de ortodoncia");
        crearBloqueo(ahora.minusHours(1), ahora.plusHours(1), "Guardia en hospital");

        mockMvc.perform(conToken(get(ruta(doctor.getId())), tokenCliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].motivo").value("Guardia en hospital"))
                .andExpect(jsonPath("$[0].inicio").value(ahora.minusHours(1).toString() + ":00"))
                .andExpect(jsonPath("$[0].fin").value(ahora.plusHours(1).toString() + ":00"))
                .andExpect(jsonPath("$[1].motivo").value("Congreso de ortodoncia"))
                .andExpect(jsonPath("$[0].idBloqueo").doesNotExist());
    }

    @Test
    void adminYDoctorTambienLosVen() throws Exception {
        crearBloqueo(ahora.plusDays(1), ahora.plusDays(1).plusHours(3), "Capacitacion");

        for (String token : new String[] {tokenAdmin, tokenDoctor}) {
            mockMvc.perform(conToken(get(ruta(doctor.getId())), token))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$", hasSize(1)))
                    .andExpect(jsonPath("$[0].motivo").value("Capacitacion"));
        }
    }

    @Test
    void sinBloqueosVigentesDevuelveListaVacia() throws Exception {
        crearBloqueo(ahora.minusDays(5), ahora.minusDays(4), "Viaje");

        mockMvc.perform(conToken(get(ruta(doctor.getId())), tokenCliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void doctorInexistenteDevuelve404() throws Exception {
        mockMvc.perform(conToken(get(ruta(UUID.randomUUID())), tokenCliente))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("Doctor no encontrado"));
    }

    @Test
    void sinTokenDevuelve401() throws Exception {
        mockMvc.perform(get(ruta(doctor.getId())))
                .andExpect(status().isUnauthorized());
    }

    private void crearBloqueo(LocalDateTime inicio, LocalDateTime fin, String motivo) {
        bloqueoDoctorRepository.save(BloqueoDoctor.builder()
                .doctor(doctor)
                .inicio(inicio)
                .fin(fin)
                .motivo(motivo)
                .fechaCreacion(ahora)
                .build());
    }

    private String ruta(UUID idDoctor) {
        return "/doctores/" + idDoctor + "/bloqueos-vigentes";
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
}
