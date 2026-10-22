package com.teo1.clinicadental.controller;

import com.teo1.clinicadental.model.AbonoPlan;
import com.teo1.clinicadental.model.Cita;
import com.teo1.clinicadental.model.Cliente;
import com.teo1.clinicadental.model.DiaSemana;
import com.teo1.clinicadental.model.Doctor;
import com.teo1.clinicadental.model.EstadoCita;
import com.teo1.clinicadental.model.EstadoUsuario;
import com.teo1.clinicadental.model.Horario;
import com.teo1.clinicadental.model.Rol;
import com.teo1.clinicadental.model.Tratamiento;
import com.teo1.clinicadental.model.Usuario;
import com.teo1.clinicadental.repository.AbonoPlanRepository;
import com.teo1.clinicadental.repository.CitaRepository;
import com.teo1.clinicadental.repository.ClienteRepository;
import com.teo1.clinicadental.repository.DoctorRepository;
import com.teo1.clinicadental.repository.HorarioRepository;
import com.teo1.clinicadental.repository.PlanTratamientoRepository;
import com.teo1.clinicadental.repository.RolRepository;
import com.teo1.clinicadental.repository.TratamientoRepository;
import com.teo1.clinicadental.repository.UsuarioRepository;
import com.teo1.clinicadental.security.JwtService;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;
import org.hibernate.SessionFactory;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PlanesTratamientoIntegrationTests {

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
    private TratamientoRepository tratamientoRepository;

    @Autowired
    private CitaRepository citaRepository;

    @Autowired
    private PlanTratamientoRepository planRepository;

    @Autowired
    private AbonoPlanRepository abonoPlanRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private Clock clock;

    @Autowired
    private EntityManager entityManager;

    private String tokenAdmin;
    private String tokenSecretaria;
    private String tokenCliente;
    private String tokenDoctor;
    private String tokenOtroDoctor;
    private Cliente cliente;
    private Doctor doctor;
    private Doctor otroDoctor;
    private Tratamiento tratamiento;
    private LocalDate fecha;

    @BeforeEach
    void setUp() {
        tokenAdmin = token(crearUsuario(Rol.ADMIN));
        tokenSecretaria = token(crearUsuario(Rol.SECRETARIA));

        Usuario usuarioCliente = crearUsuario(Rol.CLIENTE);
        cliente = clienteRepository.save(Cliente.builder().usuario(usuarioCliente).dpi(dpiAleatorio()).build());
        tokenCliente = token(usuarioCliente);

        Usuario usuarioDoctor = crearUsuario(Rol.DOCTOR);
        doctor = doctorRepository.save(Doctor.builder().usuario(usuarioDoctor).especialidad("Endodoncia").build());
        tokenDoctor = token(usuarioDoctor);

        Usuario usuarioOtroDoctor = crearUsuario(Rol.DOCTOR);
        otroDoctor = doctorRepository.save(Doctor.builder().usuario(usuarioOtroDoctor).especialidad("Ortodoncia").build());
        tokenOtroDoctor = token(usuarioOtroDoctor);

        tratamiento = tratamientoRepository.save(Tratamiento.builder()
                .nombre("Tratamiento de prueba " + UUID.randomUUID())
                .duracionMinutos(60)
                .costo(new BigDecimal("300.00"))
                .build());

        fecha = LocalDate.now(clock).plusDays(7);
        horarioRepository.save(Horario.builder()
                .doctor(doctor)
                .diaSemana(DiaSemana.desde(fecha.getDayOfWeek()))
                .horaInicio(LocalTime.of(8, 0))
                .horaFin(LocalTime.of(12, 0))
                .build());
    }

    @Test
    void secretariaCreaUnPlanConElCostoDelCatalogoPorDefecto() throws Exception {
        crearPlan(tokenSecretaria, doctor.getId(), 3, null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idCliente").value(cliente.getId().toString()))
                .andExpect(jsonPath("$.idDoctor").value(doctor.getId().toString()))
                .andExpect(jsonPath("$.nombreTratamiento").value(tratamiento.getNombre()))
                .andExpect(jsonPath("$.estado").value("ACTIVO"))
                .andExpect(jsonPath("$.totalSesiones").value(3))
                .andExpect(jsonPath("$.sesionesPendientes").value(3))
                .andExpect(jsonPath("$.porcentajeAvance").value(0))
                .andExpect(jsonPath("$.costoTotal").value(900.00))
                .andExpect(jsonPath("$.totalAbonado").value(0))
                .andExpect(jsonPath("$.saldo").value(900.00))
                .andExpect(jsonPath("$.citas").doesNotExist());

        crearPlan(tokenAdmin, doctor.getId(), 2, "750.50")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.costoTotal").value(750.50));
    }

    @Test
    void elDoctorCreaPlanesSoloParaSiMismo() throws Exception {
        crearPlan(tokenDoctor, null, 2, null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idDoctor").value(doctor.getId().toString()));

        crearPlan(tokenDoctor, otroDoctor.getId(), 2, null)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("Solo puedes crear planes para ti mismo"));
        crearPlan(tokenCliente, doctor.getId(), 2, null).andExpect(status().isForbidden());
    }

    @Test
    void datosInvalidosAlCrearSeRechazan() throws Exception {
        crearPlan(tokenSecretaria, null, 2, null)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Debes indicar el doctor del plan"));
        crearPlan(tokenSecretaria, doctor.getId(), 0, null).andExpect(status().isBadRequest());
        crearPlan(tokenSecretaria, doctor.getId(), 2, "-1").andExpect(status().isBadRequest());
        crearPlan(tokenSecretaria, UUID.randomUUID(), 2, null).andExpect(status().isNotFound());

        tratamiento.setActivo(false);
        crearPlan(tokenSecretaria, doctor.getId(), 2, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("El tratamiento no esta activo en el catalogo"));
    }

    @Test
    void noSeAgendanMasSesionesQueLasDelPlan() throws Exception {
        String idPlan = idPlan(crearPlan(tokenSecretaria, doctor.getId(), 2, null));

        agendarSesion(tokenSecretaria, idPlan, "09:00")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idCliente").value(cliente.getId().toString()))
                .andExpect(jsonPath("$.idDoctor").value(doctor.getId().toString()))
                .andExpect(jsonPath("$.estado").value("AGENDADA"))
                .andExpect(jsonPath("$.notas").value("Sesion 1 de 2: " + tratamiento.getNombre()));
        agendarSesion(tokenAdmin, idPlan, "10:00").andExpect(status().isCreated());

        agendarSesion(tokenSecretaria, idPlan, "11:00")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("Ya se agendaron las 2 sesiones del plan"));

        obtenerPlan(tokenSecretaria, idPlan)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sesionesAgendadas").value(2))
                .andExpect(jsonPath("$.sesionesPendientes").value(0))
                .andExpect(jsonPath("$.citas", hasSize(2)))
                .andExpect(jsonPath("$.citas[0].hora").value("09:00:00"))
                .andExpect(jsonPath("$.citas[1].hora").value("10:00:00"));
    }

    @Test
    void unaSesionCanceladaLiberaSuLugar() throws Exception {
        String idPlan = idPlan(crearPlan(tokenSecretaria, doctor.getId(), 1, null));
        String idCita = idCita(agendarSesion(tokenSecretaria, idPlan, "09:00").andExpect(status().isCreated()));
        agendarSesion(tokenSecretaria, idPlan, "10:00").andExpect(status().isConflict());

        cambiarEstado(idCita, "CANCELADA");

        agendarSesion(tokenSecretaria, idPlan, "10:00").andExpect(status().isCreated());
    }

    @Test
    void elAvanceEsAtendidasSobreTotalesYAlTerminarQuedaCompletado() throws Exception {
        String idPlan = idPlan(crearPlan(tokenSecretaria, doctor.getId(), 2, null));
        String primera = idCita(agendarSesion(tokenSecretaria, idPlan, "09:00"));
        String segunda = idCita(agendarSesion(tokenSecretaria, idPlan, "10:00"));

        cambiarEstado(primera, "ATENDIDA");
        obtenerPlan(tokenDoctor, idPlan)
                .andExpect(jsonPath("$.sesionesAtendidas").value(1))
                .andExpect(jsonPath("$.sesionesAgendadas").value(1))
                .andExpect(jsonPath("$.porcentajeAvance").value(50))
                .andExpect(jsonPath("$.estado").value("ACTIVO"));

        cambiarEstado(segunda, "ATENDIDA");
        obtenerPlan(tokenDoctor, idPlan)
                .andExpect(jsonPath("$.porcentajeAvance").value(100))
                .andExpect(jsonPath("$.estado").value("COMPLETADO"));
        agendarSesion(tokenSecretaria, idPlan, "11:00")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("El plan ya esta completado"));
    }

    @Test
    void elPacienteBloqueadoPorInasistenciasNoAgendaSesiones() throws Exception {
        String idPlan = idPlan(crearPlan(tokenSecretaria, doctor.getId(), 3, null));
        for (int i = 1; i <= 3; i++) {
            citaRepository.save(Cita.builder()
                    .cliente(cliente)
                    .doctor(doctor)
                    .fecha(LocalDate.now(clock).minusDays(30 + i))
                    .hora(LocalTime.of(9, 0))
                    .estado(EstadoCita.NO_ASISTIO)
                    .build());
        }

        agendarSesion(tokenSecretaria, idPlan, "09:00")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("El paciente tiene 3 inasistencias y no puede agendar citas "
                        + "hasta que secretaria o administracion le levanten la restriccion"));
        obtenerPlan(tokenSecretaria, idPlan).andExpect(jsonPath("$.citas", hasSize(0)));
    }

    @Test
    void noSeAgendaUnaSesionDentroDeUnBloqueoDelDoctor() throws Exception {
        String idPlan = idPlan(crearPlan(tokenSecretaria, doctor.getId(), 3, null));
        mockMvc.perform(conToken(post("/doctores/" + doctor.getId() + "/bloqueos"), tokenSecretaria)
                        .content(String.format("{\"inicio\":\"%sT10:00:00\",\"fin\":\"%sT11:00:00\",\"motivo\":\"Congreso\"}",
                                fecha, fecha)))
                .andExpect(status().isCreated());

        agendarSesion(tokenSecretaria, idPlan, "10:00")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje").value("El doctor no atiende en ese horario: Congreso"));
        agendarSesion(tokenSecretaria, idPlan, "11:00").andExpect(status().isCreated());
    }

    @Test
    void elSaldoEsElCostoTotalMenosLosAbonos() throws Exception {
        String idPlan = idPlan(crearPlan(tokenSecretaria, doctor.getId(), 2, "1000.00"));
        for (String monto : new String[] {"250.00", "100.50"}) {
            abonoPlanRepository.save(AbonoPlan.builder()
                    .plan(planRepository.getReferenceById(UUID.fromString(idPlan)))
                    .monto(new BigDecimal(monto))
                    .fecha(LocalDateTime.now(clock))
                    .build());
        }

        obtenerPlan(tokenSecretaria, idPlan)
                .andExpect(jsonPath("$.totalAbonado").value(350.50))
                .andExpect(jsonPath("$.saldo").value(649.50));
    }

    @Test
    void elDoctorSoloVeSusPlanesYLosFiltrosAplican() throws Exception {
        String idPlan = idPlan(crearPlan(tokenSecretaria, doctor.getId(), 2, null));
        crearPlan(tokenSecretaria, otroDoctor.getId(), 2, null).andExpect(status().isCreated());

        mockMvc.perform(conToken(get("/planes"), tokenDoctor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].idPlan").value(idPlan));
        mockMvc.perform(conToken(get("/planes").param("idCliente", cliente.getId().toString())
                                .param("idDoctor", otroDoctor.getId().toString()), tokenSecretaria))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].idDoctor").value(otroDoctor.getId().toString()));
        mockMvc.perform(conToken(get("/planes").param("idCliente", UUID.randomUUID().toString()), tokenAdmin))
                .andExpect(jsonPath("$", hasSize(0)));

        obtenerPlan(tokenOtroDoctor, idPlan)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value("Solo puedes ver tus propios planes"));
        obtenerPlan(tokenSecretaria, UUID.randomUUID().toString()).andExpect(status().isNotFound());
        agendarSesion(tokenSecretaria, UUID.randomUUID().toString(), "09:00").andExpect(status().isNotFound());
        agendarSesion(tokenDoctor, idPlan, "09:00").andExpect(status().isForbidden());
    }

    @Test
    void adminYSecretariaRegistranAbonosConFechaYUsuarioDelServidor() throws Exception {
        String id = idPlan(crearPlan(tokenSecretaria, doctor.getId(), 2, "100.00"));
        abonos(tokenAdmin, id).andExpect(status().isOk())
                .andExpect(jsonPath("$.costoTotal").value(100.0))
                .andExpect(jsonPath("$.totalAbonado").value(0))
                .andExpect(jsonPath("$.saldo").value(100.0))
                .andExpect(jsonPath("$.abonos").isEmpty());

        mockMvc.perform(conToken(post("/planes/" + id + "/abonos"), tokenAdmin)
                        .content("{\"monto\":0.01,\"fecha\":\"1999-01-01T00:00:00\",\"registradoPor\":\"" + doctor.getUsuario().getId() + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.idAbono").exists())
                .andExpect(jsonPath("$.idPlan").value(id))
                .andExpect(jsonPath("$.monto").value(0.01))
                .andExpect(jsonPath("$.idRegistradoPor").value(jwtService.extractSubject(tokenAdmin)))
                .andExpect(jsonPath("$.fecha").value(org.hamcrest.Matchers.startsWith(LocalDate.now(clock).toString())));
        abonar(tokenSecretaria, id, "99.99").andExpect(status().isCreated())
                .andExpect(jsonPath("$.idRegistradoPor").value(jwtService.extractSubject(tokenSecretaria)));
        abonos(tokenSecretaria, id).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAbonado").value(100.0))
                .andExpect(jsonPath("$.saldo").value(0))
                .andExpect(jsonPath("$.abonos", hasSize(2)));
        obtenerPlan(tokenAdmin, id).andExpect(jsonPath("$.saldo").value(0));
        abonar(tokenAdmin, id, "0.01").andExpect(status().isConflict());
    }

    @Test
    void abonosInvalidosOExcesivosNoSeRegistran() throws Exception {
        String id = idPlan(crearPlan(tokenSecretaria, doctor.getId(), 2, "10.00"));
        for (String monto : new String[] {"null", "0", "-1", "0.001", "100000000.00", "\"invalido\""}) {
            abonar(tokenAdmin, id, monto).andExpect(status().isBadRequest());
        }
        mockMvc.perform(conToken(post("/planes/" + id + "/abonos"), tokenAdmin).content("{}"))
                .andExpect(status().isBadRequest());
        abonar(tokenAdmin, id, "10.01").andExpect(status().isConflict());
        abonos(tokenAdmin, id).andExpect(jsonPath("$.abonos").isEmpty())
                .andExpect(jsonPath("$.saldo").value(10.0));
        String gratuito = idPlan(crearPlan(tokenAdmin, doctor.getId(), 1, "0.00"));
        abonar(tokenAdmin, gratuito, "0.01").andExpect(status().isConflict());
        String maximo = idPlan(crearPlan(tokenAdmin, doctor.getId(), 1, "99999999.99"));
        abonar(tokenAdmin, maximo, "99999999.99").andExpect(status().isCreated());
        abonos(tokenAdmin, maximo).andExpect(jsonPath("$.saldo").value(0));
        abonar(tokenAdmin, UUID.randomUUID().toString(), "1.00").andExpect(status().isNotFound());
        abonos(tokenAdmin, UUID.randomUUID().toString()).andExpect(status().isNotFound());
        abonar(tokenAdmin, "invalido", "1.00").andExpect(status().isBadRequest());
        abonos(tokenAdmin, "invalido").andExpect(status().isBadRequest());
    }

    @Test
    void abonosRespetanRolesYLaPropiedadDelDoctor() throws Exception {
        String id = idPlan(crearPlan(tokenSecretaria, doctor.getId(), 2, "100.00"));
        for (String token : new String[] {tokenDoctor, tokenOtroDoctor, tokenCliente}) {
            abonar(token, id, "10.00").andExpect(status().isForbidden());
        }
        abonos(tokenDoctor, id).andExpect(status().isOk());
        abonos(tokenOtroDoctor, id).andExpect(status().isForbidden());
        abonos(tokenCliente, id).andExpect(status().isForbidden());
        mockMvc.perform(get("/planes/" + id + "/abonos")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/planes/" + id + "/abonos").contentType(MediaType.APPLICATION_JSON).content("{\"monto\":1}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void misPlanesUsaSoloElJwtYReutilizaAvanceYSaldo() throws Exception {
        String propio = idPlan(crearPlan(tokenAdmin, doctor.getId(), 4, "1000.00"));
        String otroPropio = idPlan(crearPlan(tokenAdmin, doctor.getId(), 2, "250.00"));
        Cliente otroCliente = clienteRepository.save(Cliente.builder().usuario(crearUsuario(Rol.CLIENTE)).dpi(dpiAleatorio()).build());
        String ajeno = idPlan(crearPlan(tokenAdmin, doctor.getId(), 2, "2000.00"));
        planRepository.findById(UUID.fromString(ajeno)).orElseThrow().setCliente(otroCliente);
        citaRepository.save(Cita.builder().cliente(cliente).doctor(doctor)
                .plan(planRepository.getReferenceById(UUID.fromString(propio)))
                .fecha(fecha).hora(LocalTime.of(9, 0)).estado(EstadoCita.ATENDIDA).build());
        abonar(tokenSecretaria, propio, "100.50").andExpect(status().isCreated());
        mockMvc.perform(conToken(get("/mis-planes").param("idCliente", otroCliente.getId().toString()), tokenCliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].idPlan", hasItems(propio, otroPropio)))
                .andExpect(jsonPath("$[?(@.idPlan == '" + propio + "')].porcentajeAvance", contains(25)))
                .andExpect(jsonPath("$[?(@.idPlan == '" + propio + "')].totalAbonado", contains(100.50)))
                .andExpect(jsonPath("$[?(@.idPlan == '" + propio + "')].saldo", contains(899.50)))
                .andExpect(jsonPath("$[0].citas").doesNotExist());
        mockMvc.perform(conToken(get("/mis-planes"), token(otroCliente.getUsuario())))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].idPlan").value(ajeno));
    }

    @Test
    void misPlanesEsSoloParaClientesConPerfilYDeSoloLectura() throws Exception {
        mockMvc.perform(conToken(get("/mis-planes"), tokenCliente))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
        for (String token : new String[] {tokenAdmin, tokenSecretaria, tokenDoctor}) {
            mockMvc.perform(conToken(get("/mis-planes"), token)).andExpect(status().isForbidden());
        }
        mockMvc.perform(get("/mis-planes")).andExpect(status().isUnauthorized());
        mockMvc.perform(conToken(get("/mis-planes"), "invalido")).andExpect(status().isUnauthorized());
        mockMvc.perform(conToken(get("/mis-planes"), token(crearUsuario(Rol.CLIENTE))))
                .andExpect(status().isNotFound());
        crearPlan(tokenCliente, doctor.getId(), 1, "100").andExpect(status().isForbidden());
        mockMvc.perform(conToken(post("/mis-planes"), tokenCliente)).andExpect(status().isMethodNotAllowed());
    }

    @Test
    void misPlanesYAbonosNoConsultanRelacionesPorCadaFila() throws Exception {
        String id = null;
        for (int i = 0; i < 8; i++) {
            id = idPlan(crearPlan(tokenAdmin, doctor.getId(), 2, "100.00"));
        }
        for (int i = 0; i < 8; i++) {
            abonar(i % 2 == 0 ? tokenAdmin : tokenSecretaria, id, "1.00").andExpect(status().isCreated());
        }
        entityManager.flush();
        entityManager.clear();
        var statistics = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        boolean enabled = statistics.isStatisticsEnabled();
        statistics.setStatisticsEnabled(true);
        try {
            statistics.clear();
            mockMvc.perform(conToken(get("/mis-planes"), tokenCliente))
                    .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(8)));
            assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(7);
            entityManager.clear();
            statistics.clear();
            abonos(tokenSecretaria, id).andExpect(status().isOk()).andExpect(jsonPath("$.abonos", hasSize(8)));
            assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(7);
        } finally {
            statistics.setStatisticsEnabled(enabled);
        }
    }

    private ResultActions abonar(String token, String id, String monto) throws Exception {
        return mockMvc.perform(conToken(post("/planes/" + id + "/abonos"), token).content("{\"monto\":" + monto + "}"));
    }

    private ResultActions abonos(String token, String id) throws Exception {
        return mockMvc.perform(conToken(get("/planes/" + id + "/abonos"), token));
    }

    private ResultActions crearPlan(String token, UUID idDoctor, int sesiones, String costo) throws Exception {
        String cuerpo = String.format("{\"idCliente\":\"%s\",\"idTratamiento\":\"%s\",\"totalSesiones\":%d%s%s}",
                cliente.getId(), tratamiento.getId(), sesiones,
                idDoctor != null ? ",\"idDoctor\":\"" + idDoctor + "\"" : "",
                costo != null ? ",\"costoTotal\":" + costo : "");
        return mockMvc.perform(conToken(post("/planes"), token).content(cuerpo));
    }

    private ResultActions obtenerPlan(String token, String idPlan) throws Exception {
        return mockMvc.perform(conToken(get("/planes/" + idPlan), token));
    }

    private ResultActions agendarSesion(String token, String idPlan, String hora) throws Exception {
        return mockMvc.perform(conToken(post("/planes/" + idPlan + "/citas"), token)
                .content(String.format("{\"fecha\":\"%s\",\"hora\":\"%s\"}", fecha, hora)));
    }

    private void cambiarEstado(String idCita, String estado) throws Exception {
        mockMvc.perform(conToken(patch("/citas/" + idCita + "/estado"), tokenSecretaria)
                        .content("{\"estado\":\"" + estado + "\",\"motivo\":\"Prueba del plan\"}"))
                .andExpect(status().isOk());
    }

    private String idPlan(ResultActions resultado) throws Exception {
        return resultado.andReturn().getResponse().getContentAsString().replaceAll(".*\"idPlan\":\"([^\"]+)\".*", "$1");
    }

    private String idCita(ResultActions resultado) throws Exception {
        return resultado.andReturn().getResponse().getContentAsString().replaceAll(".*\"idCita\":\"([^\"]+)\".*", "$1");
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
