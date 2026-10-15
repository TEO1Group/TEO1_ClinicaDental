package com.teo1.clinicadental.controller;

import com.teo1.clinicadental.model.Cliente;
import com.teo1.clinicadental.model.Doctor;
import com.teo1.clinicadental.model.EstadoUsuario;
import com.teo1.clinicadental.model.Rol;
import com.teo1.clinicadental.model.Usuario;
import com.teo1.clinicadental.repository.ClienteRepository;
import com.teo1.clinicadental.repository.DoctorRepository;
import com.teo1.clinicadental.repository.RolRepository;
import com.teo1.clinicadental.repository.UsuarioRepository;
import com.teo1.clinicadental.security.JwtService;
import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Los listados de pacientes y doctores deben hacer el mismo numero de consultas SQL sin importar
 * cuantos registros haya (sin N+1 al leer el usuario de cada uno).
 */
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@AutoConfigureMockMvc
@Transactional
class ConsultasListadosIntegrationTests {

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
    private JwtService jwtService;

    @Autowired
    private EntityManager entityManager;

    private Statistics estadisticas;
    private String tokenAdmin;

    @BeforeEach
    void setUp() {
        estadisticas = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        Usuario admin = crearUsuario(Rol.ADMIN);
        tokenAdmin = jwtService.generateToken(admin.getId(), admin.getRol().getNombreRol());
    }

    @Test
    void listarPacientesNoHaceUnaConsultaPorPaciente() throws Exception {
        crearPacientes(3);
        long conTres = consultasAl("/pacientes");

        crearPacientes(5);
        long conOcho = consultasAl("/pacientes");

        assertEquals(conTres, conOcho);
    }

    @Test
    void listarDoctoresNoHaceUnaConsultaPorDoctor() throws Exception {
        crearDoctores(3);
        long conTres = consultasAl("/doctores");

        crearDoctores(5);
        long conOcho = consultasAl("/doctores");

        assertEquals(conTres, conOcho);
    }

    private long consultasAl(String ruta) throws Exception {
        // Se vacia el contexto de persistencia para que cada entidad se lea de la base y no de memoria
        entityManager.flush();
        entityManager.clear();
        estadisticas.clear();

        mockMvc.perform(get(ruta).header("Authorization", "Bearer " + tokenAdmin))
                .andExpect(status().isOk());

        return estadisticas.getPrepareStatementCount();
    }

    private void crearPacientes(int cantidad) {
        for (int i = 0; i < cantidad; i++) {
            clienteRepository.save(Cliente.builder().usuario(crearUsuario(Rol.CLIENTE)).dpi(dpiAleatorio()).build());
        }
    }

    private void crearDoctores(int cantidad) {
        for (int i = 0; i < cantidad; i++) {
            doctorRepository.save(Doctor.builder().usuario(crearUsuario(Rol.DOCTOR)).especialidad("General").build());
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

    private String dpiAleatorio() {
        return String.format("%013d", Math.abs(UUID.randomUUID().getMostSignificantBits()) % 10_000_000_000_000L);
    }
}
