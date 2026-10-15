package com.teo1.clinicadental.controller;

import com.teo1.clinicadental.model.EstadoUsuario;
import com.teo1.clinicadental.model.Rol;
import com.teo1.clinicadental.model.Usuario;
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

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Reglas de SecurityConfig para los endpoints del Sprint 5. Solo se prueban los rechazos por rol,
 * que no cambian cuando se implementan los controllers; los permisos finos van en cada servicio.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SeguridadSprint5IntegrationTests {

    private static final String ID = UUID.randomUUID().toString();

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private JwtService jwtService;

    private String tokenCliente;
    private String tokenDoctor;
    private String tokenSecretaria;
    private String tokenAdmin;

    @BeforeEach
    void setUp() {
        tokenCliente = token(crearUsuario(Rol.CLIENTE));
        tokenDoctor = token(crearUsuario(Rol.DOCTOR));
        tokenSecretaria = token(crearUsuario(Rol.SECRETARIA));
        tokenAdmin = token(crearUsuario(Rol.ADMIN));
    }

    @Test
    void reprogramarRequiereSesion() throws Exception {
        mockMvc.perform(patch("/citas/" + ID + "/reprogramar").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void soloAdminYSecretariaCreanYBorranBloqueos() throws Exception {
        for (String token : new String[] {tokenCliente, tokenDoctor}) {
            mockMvc.perform(conToken(post("/doctores/" + ID + "/bloqueos"), token).content("{}"))
                    .andExpect(status().isForbidden());
            mockMvc.perform(conToken(delete("/doctores/" + ID + "/bloqueos/" + ID), token))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void soloElDoctorRegistraTratamientos() throws Exception {
        for (String token : new String[] {tokenCliente, tokenSecretaria, tokenAdmin}) {
            mockMvc.perform(conToken(post("/citas/" + ID + "/tratamientos"), token).content("{}"))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void elPacienteNoVeTratamientos() throws Exception {
        mockMvc.perform(conToken(get("/tratamientos"), tokenCliente)).andExpect(status().isForbidden());
        mockMvc.perform(conToken(get("/citas/" + ID + "/tratamientos"), tokenCliente)).andExpect(status().isForbidden());
        mockMvc.perform(conToken(get("/pacientes/" + ID + "/tratamientos"), tokenCliente)).andExpect(status().isForbidden());
    }

    @Test
    void soloAdminYSecretariaVenReportes() throws Exception {
        for (String token : new String[] {tokenCliente, tokenDoctor}) {
            mockMvc.perform(conToken(get("/reportes/citas-por-doctor"), token))
                    .andExpect(status().isForbidden());
        }
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
