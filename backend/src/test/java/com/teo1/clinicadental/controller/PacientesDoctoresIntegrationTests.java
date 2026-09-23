package com.teo1.clinicadental.controller;

import com.teo1.clinicadental.model.Doctor;
import com.teo1.clinicadental.model.EstadoUsuario;
import com.teo1.clinicadental.model.Rol;
import com.teo1.clinicadental.model.Usuario;
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

import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PacientesDoctoresIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private JwtService jwtService;

    private Usuario admin;
    private Usuario secretaria;
    private String tokenAdmin;
    private String tokenSecretaria;
    private String tokenCliente;
    private String tokenDoctor;
    private String tokenOtroDoctor;
    private Doctor doctor;
    private Doctor otroDoctor;

    @BeforeEach
    void setUp() {
        admin = crearUsuario(Rol.ADMIN);
        tokenAdmin = token(admin);
        secretaria = crearUsuario(Rol.SECRETARIA);
        tokenSecretaria = token(secretaria);
        tokenCliente = token(crearUsuario(Rol.CLIENTE));

        Usuario usuarioDoctor = crearUsuario(Rol.DOCTOR);
        doctor = doctorRepository.save(Doctor.builder().usuario(usuarioDoctor).especialidad("Ortodoncia").build());
        tokenDoctor = token(usuarioDoctor);

        Usuario usuarioOtroDoctor = crearUsuario(Rol.DOCTOR);
        otroDoctor = doctorRepository.save(Doctor.builder().usuario(usuarioOtroDoctor).especialidad("Endodoncia").build());
        tokenOtroDoctor = token(usuarioOtroDoctor);
    }

    @Test
    void sinTokenDevuelve401ConMensaje() throws Exception {
        mockMvc.perform(get("/pacientes"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Debes iniciar sesion"));
    }

    @Test
    void clienteNoPuedeVerPacientes() throws Exception {
        mockMvc.perform(conToken(get("/pacientes"), tokenCliente))
                .andExpect(status().isForbidden());
    }

    @Test
    void validacionDevuelveErrorPorCampo() throws Exception {
        String body = pacienteJson(dpiAleatorio(), "correo-" + UUID.randomUUID() + "@prueba.com", "123");

        mockMvc.perform(conToken(post("/pacientes"), tokenSecretaria).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores.telefono").value("El teléfono debe tener 8 dígitos"))
                .andExpect(jsonPath("$.mensaje").exists());
    }

    @Test
    void secretariaCreaYDesactivaPaciente() throws Exception {
        String dpi = dpiAleatorio();
        String body = pacienteJson(dpi, "paciente-" + UUID.randomUUID() + "@prueba.com", "55554444");

        String respuesta = mockMvc.perform(conToken(post("/pacientes"), tokenSecretaria).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.dpi").value(dpi))
                .andExpect(jsonPath("$.estado").value("ACTIVO"))
                .andReturn().getResponse().getContentAsString();
        String idCliente = respuesta.replaceAll(".*\"idCliente\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(conToken(delete("/pacientes/" + idCliente), tokenSecretaria))
                .andExpect(status().isNoContent());

        mockMvc.perform(conToken(get("/pacientes"), tokenSecretaria))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].idCliente", not(hasItem(idCliente))));

        mockMvc.perform(conToken(get("/pacientes/" + idCliente), tokenSecretaria))
                .andExpect(jsonPath("$.estado").value("INACTIVO"));
    }

    @Test
    void soloAdminDesactivaDoctor() throws Exception {
        mockMvc.perform(conToken(delete("/doctores/" + doctor.getId()), tokenSecretaria))
                .andExpect(status().isForbidden());

        mockMvc.perform(conToken(delete("/doctores/" + doctor.getId()), tokenAdmin))
                .andExpect(status().isNoContent());

        mockMvc.perform(conToken(get("/doctores"), tokenAdmin))
                .andExpect(jsonPath("$[*].idDoctor", not(hasItem(doctor.getId().toString()))));
    }

    @Test
    void adminActualizaNombreYEspecialidadDelDoctor() throws Exception {
        mockMvc.perform(conToken(put("/doctores/" + doctor.getId()), tokenAdmin)
                        .content("{\"nombre\":\"Laura\",\"telefono\":\"44445555\",\"especialidad\":\"Periodoncia\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Laura"))
                .andExpect(jsonPath("$.telefono").value("44445555"))
                .andExpect(jsonPath("$.especialidad").value("Periodoncia"));
    }

    @Test
    void horarioTraslapadoDevuelve409() throws Exception {
        mockMvc.perform(conToken(post("/doctores/" + doctor.getId() + "/horarios"), tokenDoctor)
                        .content(horarioJson("LUNES", "08:00", "12:00")))
                .andExpect(status().isCreated());

        mockMvc.perform(conToken(post("/doctores/" + doctor.getId() + "/horarios"), tokenDoctor)
                        .content(horarioJson("LUNES", "11:00", "13:00")))
                .andExpect(status().isConflict());

        mockMvc.perform(conToken(post("/doctores/" + doctor.getId() + "/horarios"), tokenDoctor)
                        .content(horarioJson("LUNES", "12:00", "14:00")))
                .andExpect(status().isCreated());
    }

    @Test
    void doctorNoPuedeModificarHorariosDeOtroDoctor() throws Exception {
        mockMvc.perform(conToken(post("/doctores/" + doctor.getId() + "/horarios"), tokenOtroDoctor)
                        .content(horarioJson("MARTES", "08:00", "12:00")))
                .andExpect(status().isForbidden());

        mockMvc.perform(conToken(post("/doctores/" + otroDoctor.getId() + "/horarios"), tokenOtroDoctor)
                        .content(horarioJson("MARTES", "08:00", "12:00")))
                .andExpect(status().isCreated());
    }

    @Test
    void secretariaEditaYEliminaHorario() throws Exception {
        String respuesta = mockMvc.perform(conToken(post("/doctores/" + doctor.getId() + "/horarios"), tokenSecretaria)
                        .content(horarioJson("MIERCOLES", "08:00", "10:00")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String idHorario = respuesta.replaceAll(".*\"idHorario\":\"([^\"]+)\".*", "$1");
        String rutaHorario = "/doctores/" + doctor.getId() + "/horarios/" + idHorario;

        mockMvc.perform(conToken(put(rutaHorario), tokenSecretaria)
                        .content(horarioJson("JUEVES", "14:00", "18:00")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.diaSemana").value("JUEVES"));

        mockMvc.perform(conToken(delete(rutaHorario), tokenSecretaria))
                .andExpect(status().isNoContent());

        mockMvc.perform(conToken(get("/doctores/" + doctor.getId() + "/horarios"), tokenSecretaria))
                .andExpect(jsonPath("$[*].idHorario", not(hasItem(idHorario))));
    }

    @Test
    void usuarioActualDevuelveRolYPerfil() throws Exception {
        mockMvc.perform(conToken(get("/auth/me"), tokenDoctor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value("DOCTOR"))
                .andExpect(jsonPath("$.idPerfil").value(doctor.getId().toString()));
    }

    @Test
    void adminEditaUsuario() throws Exception {
        mockMvc.perform(conToken(put("/admin/usuarios/" + secretaria.getId()), tokenAdmin)
                        .content("{\"nombre\":\"Maria\",\"telefono\":\"33332222\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Maria"))
                .andExpect(jsonPath("$.telefono").value("33332222"))
                .andExpect(jsonPath("$.rol").value("SECRETARIA"));

        mockMvc.perform(conToken(put("/admin/usuarios/" + secretaria.getId()), tokenAdmin)
                        .content("{\"email\":\"" + admin.getEmail() + "\"}"))
                .andExpect(status().isConflict());

        mockMvc.perform(conToken(put("/admin/usuarios/" + secretaria.getId()), tokenSecretaria)
                        .content("{\"nombre\":\"Otra\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void usuarioDesactivadoPierdeAcceso() throws Exception {
        mockMvc.perform(conToken(get("/pacientes"), tokenSecretaria))
                .andExpect(status().isOk());

        mockMvc.perform(conToken(patch("/admin/usuarios/" + secretaria.getId() + "/estado"), tokenAdmin)
                        .content("{\"estado\":\"INACTIVO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("INACTIVO"));

        mockMvc.perform(conToken(get("/pacientes"), tokenSecretaria))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(conToken(patch("/admin/usuarios/" + secretaria.getId() + "/estado"), tokenAdmin)
                        .content("{\"estado\":\"ACTIVO\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(conToken(get("/pacientes"), tokenSecretaria))
                .andExpect(status().isOk());
    }

    @Test
    void adminNoPuedeDesactivarseASiMismo() throws Exception {
        mockMvc.perform(conToken(patch("/admin/usuarios/" + admin.getId() + "/estado"), tokenAdmin)
                        .content("{\"estado\":\"INACTIVO\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("No puedes desactivar tu propia cuenta"));
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

    private String pacienteJson(String dpi, String email, String telefono) {
        return String.format(
                "{\"dpi\":\"%s\",\"nombre\":\"Pepe\",\"apellido\":\"Perez\",\"email\":\"%s\","
                        + "\"password\":\"Paciente1!\",\"telefono\":\"%s\"}",
                dpi, email, telefono
        );
    }

    private String horarioJson(String dia, String inicio, String fin) {
        return String.format("{\"diaSemana\":\"%s\",\"horaInicio\":\"%s\",\"horaFin\":\"%s\"}", dia, inicio, fin);
    }

    private String dpiAleatorio() {
        return String.format("%013d", Math.abs(UUID.randomUUID().getMostSignificantBits()) % 10_000_000_000_000L);
    }
}
