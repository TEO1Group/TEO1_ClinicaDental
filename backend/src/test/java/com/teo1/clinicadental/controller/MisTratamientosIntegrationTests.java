package com.teo1.clinicadental.controller;

import com.teo1.clinicadental.model.Cita;
import com.teo1.clinicadental.model.CitaTratamiento;
import com.teo1.clinicadental.model.Cliente;
import com.teo1.clinicadental.model.Doctor;
import com.teo1.clinicadental.model.EstadoCita;
import com.teo1.clinicadental.model.EstadoUsuario;
import com.teo1.clinicadental.model.Rol;
import com.teo1.clinicadental.model.Tratamiento;
import com.teo1.clinicadental.model.Usuario;
import com.teo1.clinicadental.repository.CitaRepository;
import com.teo1.clinicadental.repository.CitaTratamientoRepository;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MisTratamientosIntegrationTests {

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
    private CitaTratamientoRepository citaTratamientoRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private Clock clock;

    private String tokenCliente;
    private String tokenOtroCliente;
    private String tokenAdmin;
    private String tokenDoctor;
    private String tokenSecretaria;
    private Doctor doctor;
    private Tratamiento tratamiento;
    private Cita citaReciente;
    private Cita citaAntigua;
    private Cita citaDelOtroCliente;

    @BeforeEach
    void setUp() {
        tokenAdmin = token(crearUsuario(Rol.ADMIN));
        tokenSecretaria = token(crearUsuario(Rol.SECRETARIA));

        Usuario usuarioDoctor = crearUsuario(Rol.DOCTOR);
        doctor = doctorRepository.save(Doctor.builder().usuario(usuarioDoctor).especialidad("Ortodoncia").build());
        tokenDoctor = token(usuarioDoctor);

        Usuario usuarioCliente = crearUsuario(Rol.CLIENTE);
        Cliente cliente = clienteRepository.save(Cliente.builder().usuario(usuarioCliente).dpi(dpiAleatorio()).build());
        tokenCliente = token(usuarioCliente);

        Usuario usuarioOtroCliente = crearUsuario(Rol.CLIENTE);
        Cliente otroCliente = clienteRepository.save(
                Cliente.builder().usuario(usuarioOtroCliente).dpi(dpiAleatorio()).build());
        tokenOtroCliente = token(usuarioOtroCliente);

        tratamiento = tratamientoRepository.save(Tratamiento.builder()
                .nombre("Tratamiento de prueba " + UUID.randomUUID())
                .descripcion("Solo para pruebas")
                .duracionMinutos(30)
                .costo(new BigDecimal("300.00"))
                .build());

        citaReciente = crearCitaAtendida(cliente, LocalDate.now(clock).minusDays(3));
        citaAntigua = crearCitaAtendida(cliente, LocalDate.now(clock).minusDays(5));
        citaDelOtroCliente = crearCitaAtendida(otroCliente, LocalDate.now(clock).minusDays(4));

        registrarTratamiento(citaAntigua, "150.25", null);
        registrarTratamiento(citaReciente, "300.00", "Pieza 36");
        registrarTratamiento(citaDelOtroCliente, "999.99", null);
    }

    @Test
    void elClienteVeSoloSusTratamientosYElTotal() throws Exception {
        mockMvc.perform(conToken(get("/mis-tratamientos"), tokenCliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(450.25))
                .andExpect(jsonPath("$.tratamientos", hasSize(2)))
                .andExpect(jsonPath("$.tratamientos[0].idCita").value(citaReciente.getId().toString()))
                .andExpect(jsonPath("$.tratamientos[0].fechaCita").value(citaReciente.getFecha().toString()))
                .andExpect(jsonPath("$.tratamientos[0].idTratamiento").value(tratamiento.getId().toString()))
                .andExpect(jsonPath("$.tratamientos[0].nombreTratamiento").value(tratamiento.getNombre()))
                .andExpect(jsonPath("$.tratamientos[0].nombreDoctor").value("Prueba"))
                .andExpect(jsonPath("$.tratamientos[0].apellidoDoctor").value("DOCTOR"))
                .andExpect(jsonPath("$.tratamientos[0].costo").value(300.0))
                .andExpect(jsonPath("$.tratamientos[0].observaciones").value("Pieza 36"))
                .andExpect(jsonPath("$.tratamientos[1].idCita").value(citaAntigua.getId().toString()))
                .andExpect(jsonPath("$.tratamientos[*].idCita", not(hasItem(citaDelOtroCliente.getId().toString()))));
    }

    @Test
    void otroClienteNoVeLosTratamientosAjenos() throws Exception {
        mockMvc.perform(conToken(get("/mis-tratamientos"), tokenOtroCliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(999.99))
                .andExpect(jsonPath("$.tratamientos", hasSize(1)))
                .andExpect(jsonPath("$.tratamientos[0].idCita").value(citaDelOtroCliente.getId().toString()));
    }

    @Test
    void clienteSinTratamientosTieneTotalCero() throws Exception {
        Usuario usuarioNuevo = crearUsuario(Rol.CLIENTE);
        clienteRepository.save(Cliente.builder().usuario(usuarioNuevo).dpi(dpiAleatorio()).build());

        mockMvc.perform(conToken(get("/mis-tratamientos"), token(usuarioNuevo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.tratamientos", hasSize(0)));
    }

    @Test
    void usuarioClienteSinPerfilDevuelve404() throws Exception {
        mockMvc.perform(conToken(get("/mis-tratamientos"), token(crearUsuario(Rol.CLIENTE))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje").value("El usuario no tiene un perfil de paciente"));
    }

    @Test
    void adminDoctorYSecretariaReciben403() throws Exception {
        for (String token : new String[] {tokenAdmin, tokenDoctor, tokenSecretaria}) {
            mockMvc.perform(conToken(get("/mis-tratamientos"), token))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void sinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/mis-tratamientos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void elClienteNoRegistraTratamientos() throws Exception {
        mockMvc.perform(conToken(post("/citas/" + citaReciente.getId() + "/tratamientos"), tokenCliente)
                        .content(String.format("{\"idTratamiento\":\"%s\"}", tratamiento.getId())))
                .andExpect(status().isForbidden());

        mockMvc.perform(conToken(get("/mis-tratamientos"), tokenCliente))
                .andExpect(jsonPath("$.tratamientos", hasSize(2)));
    }

    private Cita crearCitaAtendida(Cliente cliente, LocalDate fecha) {
        return citaRepository.save(Cita.builder()
                .cliente(cliente)
                .doctor(doctor)
                .fecha(fecha)
                .hora(LocalTime.of(9, 0))
                .estado(EstadoCita.ATENDIDA)
                .build());
    }

    private void registrarTratamiento(Cita cita, String costo, String observaciones) {
        citaTratamientoRepository.save(CitaTratamiento.builder()
                .cita(cita)
                .tratamiento(tratamiento)
                .costo(new BigDecimal(costo))
                .observaciones(observaciones)
                .fechaRegistro(LocalDateTime.now(clock))
                .build());
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
