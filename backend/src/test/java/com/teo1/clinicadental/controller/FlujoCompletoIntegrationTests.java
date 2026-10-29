package com.teo1.clinicadental.controller;

import com.jayway.jsonpath.JsonPath;
import com.teo1.clinicadental.model.DiaSemana;
import com.teo1.clinicadental.model.EstadoUsuario;
import com.teo1.clinicadental.model.Rol;
import com.teo1.clinicadental.model.Usuario;
import com.teo1.clinicadental.repository.RolRepository;
import com.teo1.clinicadental.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Flujo completo de la HU-34 (#255) solo por la API, con login real de cada rol: el administrador da de alta a la
 * secretaria, al doctor con su horario y un tratamiento; la secretaria registra al paciente y le agenda una cita;
 * el doctor la atiende y registra el tratamiento. Las fechas son relativas a hoy y el reloj se adelanta a la hora
 * de la cita para que la prueba no dependa de la hora a la que corra.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class FlujoCompletoIntegrationTests {

    private static final ZoneId ZONA = ZoneId.of("America/Guatemala");
    private static final String CLAVE = "Flujo#2026a";
    private static final LocalTime HORA_CITA = LocalTime.of(10, 0);

    @TestConfiguration
    static class RelojDePrueba {

        @Bean
        @Primary
        RelojAjustable relojAjustable() {
            return new RelojAjustable(ZONA);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RelojAjustable reloj;

    private final String sufijo = UUID.randomUUID().toString().substring(0, 8);

    @BeforeEach
    void setUp() {
        reloj.volverAlPresente();
    }

    @Test
    void registroCitaAtencionYTratamientoConLosTresRoles() throws Exception {
        // Administrador inicial, como lo crea AdminInitializer en un despliegue nuevo
        String emailAdmin = "admin-" + sufijo + "@flujo.test";
        usuarioRepository.save(Usuario.builder()
                .rol(rolRepository.findByNombreRol(Rol.ADMIN).orElseThrow())
                .nombre("Ana")
                .apellido("Admin")
                .email(emailAdmin)
                .passwordHash(passwordEncoder.encode(CLAVE))
                .estado(EstadoUsuario.ACTIVO)
                .build());
        String tokenAdmin = login(emailAdmin);

        // 1. El administrador registra a la secretaria y al doctor
        String emailSecretaria = "secretaria-" + sufijo + "@flujo.test";
        enviar(post("/admin/usuarios"), tokenAdmin, """
                {"nombre": "Sara", "apellido": "Recepcion", "email": "%s", "password": "%s",
                 "telefono": "55551111", "idRol": "SECRETARIA", "turno": "MANANA"}
                """.formatted(emailSecretaria, CLAVE), status().isCreated());
        String tokenSecretaria = login(emailSecretaria);

        String emailDoctor = "doctor-" + sufijo + "@flujo.test";
        String idUsuarioDoctor = leer(enviar(post("/admin/usuarios"), tokenAdmin, """
                {"nombre": "Diego", "apellido": "Molar", "email": "%s", "password": "%s",
                 "telefono": "55552222", "idRol": "DOCTOR", "especialidad": "Odontologia general",
                 "numeroColegiado": "COL-%s"}
                """.formatted(emailDoctor, CLAVE, sufijo), status().isCreated()), "$.id");
        String tokenDoctor = login(emailDoctor);

        List<String> idsDoctor = JsonPath.read(enviar(get("/doctores"), tokenAdmin, null, status().isOk()),
                "$[?(@.idUsuario == '" + idUsuarioDoctor + "')].idDoctor");
        assertThat(idsDoctor).hasSize(1);
        String idDoctor = idsDoctor.get(0);

        // 2. Horario del doctor para el dia de la semana de la cita, dentro de una semana
        LocalDate fecha = LocalDate.now(reloj).plusDays(7);
        String diaSemana = DiaSemana.desde(fecha.getDayOfWeek()).name();
        enviar(post("/doctores/" + idDoctor + "/horarios"), tokenAdmin, """
                {"diaSemana": "%s", "horaInicio": "08:00", "horaFin": "12:00"}
                """.formatted(diaSemana), status().isCreated());

        // 3. Tratamiento del catalogo
        String idTratamiento = leer(enviar(post("/tratamientos"), tokenAdmin, """
                {"nombre": "Limpieza flujo %s", "descripcion": "Limpieza dental completa",
                 "duracionMinutos": 30, "costo": 350.00, "activo": true}
                """.formatted(sufijo), status().isCreated()), "$.idTratamiento");

        // 4. La secretaria registra al paciente
        String idCliente = leer(enviar(post("/pacientes"), tokenSecretaria, """
                {"dpi": "%s", "nombre": "Pedro", "apellido": "Paciente", "email": "paciente-%s@flujo.test",
                 "password": "%s", "telefono": "55553333"}
                """.formatted(dpi(), sufijo, CLAVE), status().isCreated()), "$.idCliente");

        // 5. La secretaria le agenda la cita
        String idCita = leer(enviar(post("/citas"), tokenSecretaria, """
                {"idDoctor": "%s", "idCliente": "%s", "fecha": "%s", "hora": "%s", "notas": "Primera consulta"}
                """.formatted(idDoctor, idCliente, fecha, HORA_CITA), status().isCreated()), "$.idCita");
        mockMvc.perform(conToken(get("/citas/" + idCita), tokenDoctor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("AGENDADA"))
                .andExpect(jsonPath("$.idDoctor").value(idDoctor))
                .andExpect(jsonPath("$.idCliente").value(idCliente));

        // 6. El doctor no puede atenderla antes de su hora; el dia de la cita, a las 10:05, si
        String atendida = """
                {"estado": "ATENDIDA"}
                """;
        enviar(patch("/citas/" + idCita + "/estado"), tokenDoctor, atendida, status().isConflict());
        reloj.ajustar(fecha, HORA_CITA.plusMinutes(5));
        mockMvc.perform(conToken(patch("/citas/" + idCita + "/estado"), tokenDoctor).content(atendida))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ATENDIDA"));

        // 7. El doctor registra el tratamiento aplicado
        mockMvc.perform(conToken(post("/citas/" + idCita + "/tratamientos"), tokenDoctor).content("""
                        {"idTratamiento": "%s", "observaciones": "Sin complicaciones"}
                        """.formatted(idTratamiento)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idCita").value(idCita))
                .andExpect(jsonPath("$.nombreTratamiento").value("Limpieza flujo " + sufijo))
                .andExpect(jsonPath("$.costo").value(350.00));

        // 8. La secretaria ve el tratamiento en el expediente del paciente
        mockMvc.perform(conToken(get("/pacientes/" + idCliente + "/tratamientos"), tokenSecretaria))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tratamientos.length()").value(1))
                .andExpect(jsonPath("$.tratamientos[0].idCita").value(idCita))
                .andExpect(jsonPath("$.tratamientos[0].nombreDoctor").value("Diego"))
                .andExpect(jsonPath("$.total").value(350.00));
    }

    private String login(String email) throws Exception {
        return leer(enviar(post("/auth/login"), null, """
                {"email": "%s", "password": "%s"}
                """.formatted(email, CLAVE), status().isOk()), "$.token");
    }

    private String enviar(MockHttpServletRequestBuilder request, String token, String body, ResultMatcher esperado)
            throws Exception {
        MockHttpServletRequestBuilder conDatos = conToken(request, token);
        if (body != null) {
            conDatos.content(body);
        }
        return mockMvc.perform(conDatos)
                .andExpect(esperado)
                .andReturn().getResponse().getContentAsString();
    }

    private static MockHttpServletRequestBuilder conToken(MockHttpServletRequestBuilder request, String token) {
        request.contentType(MediaType.APPLICATION_JSON);
        return token == null ? request : request.header("Authorization", "Bearer " + token);
    }

    private static String leer(String json, String ruta) {
        return JsonPath.read(json, ruta);
    }

    private static String dpi() {
        return String.format("%013d", Math.floorMod(UUID.randomUUID().getMostSignificantBits(), 10_000_000_000_000L));
    }

    /** Reloj del sistema que se puede fijar en otro instante para simular el paso del tiempo. */
    static class RelojAjustable extends Clock {

        private final ZoneId zona;
        private volatile Instant fijo;

        RelojAjustable(ZoneId zona) {
            this.zona = zona;
        }

        void ajustar(LocalDate fecha, LocalTime hora) {
            fijo = fecha.atTime(hora).atZone(zona).toInstant();
        }

        void volverAlPresente() {
            fijo = null;
        }

        @Override
        public ZoneId getZone() {
            return zona;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return zone.equals(zona) ? this : Clock.system(zone);
        }

        @Override
        public Instant instant() {
            Instant instante = fijo;
            return instante != null ? instante : Instant.now();
        }
    }
}
