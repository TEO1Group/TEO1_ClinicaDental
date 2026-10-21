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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Reglas de SecurityConfig para los endpoints de planes y abonos del Sprint 6. Solo se prueban los
 * rechazos por rol, que no cambian cuando se implementan los controllers; los permisos finos van en cada servicio.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SeguridadSprint6IntegrationTests {

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
    void planesRequierenSesion() throws Exception {
        mockMvc.perform(get("/planes")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/mis-planes")).andExpect(status().isUnauthorized());
    }

    @Test
    void elPacienteNoCreaNiConsultaPlanesAjenos() throws Exception {
        mockMvc.perform(conToken(post("/planes"), tokenCliente).content("{}")).andExpect(status().isForbidden());
        mockMvc.perform(conToken(get("/planes"), tokenCliente)).andExpect(status().isForbidden());
        mockMvc.perform(conToken(get("/planes/" + ID), tokenCliente)).andExpect(status().isForbidden());
        mockMvc.perform(conToken(get("/planes/" + ID + "/abonos"), tokenCliente)).andExpect(status().isForbidden());
    }

    @Test
    void soloAdminYSecretariaAgendanSesionesYRegistranAbonos() throws Exception {
        for (String token : new String[] {tokenCliente, tokenDoctor}) {
            mockMvc.perform(conToken(post("/planes/" + ID + "/citas"), token).content("{}"))
                    .andExpect(status().isForbidden());
            mockMvc.perform(conToken(post("/planes/" + ID + "/abonos"), token).content("{}"))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void soloElPacienteVeSusPlanes() throws Exception {
        for (String token : new String[] {tokenDoctor, tokenSecretaria, tokenAdmin}) {
            mockMvc.perform(conToken(get("/mis-planes"), token)).andExpect(status().isForbidden());
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
