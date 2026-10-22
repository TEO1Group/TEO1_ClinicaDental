package com.teo1.clinicadental.controller;

import com.jayway.jsonpath.JsonPath;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
class OpenApiDocumentationIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    @Test
    void everyControllerOperationDocumentsSecurityAndResponseSchemaPresence() throws Exception {
        String document = document();
        Map<String, Map<String, Map<String, Object>>> paths = JsonPath.read(document, "$.paths");

        Set<String> actualOperations = new HashSet<>();
        handlerMapping.getHandlerMethods().forEach((mapping, handler) -> {
            if (!handler.getBeanType().getPackageName().equals(getClass().getPackageName())) {
                return;
            }
            mapping.getPatternValues().forEach(path -> mapping.getMethodsCondition().getMethods().forEach(method -> {
                actualOperations.add(method.name().toLowerCase() + " " + path);
                Map<String, Object> operation = paths.get(path).get(method.name().toLowerCase());
                assertThat(operation).as("%s %s", method, path).isNotNull();
                assertThat(operation).containsKeys("summary", "description", "tags", "responses");

                boolean isPublic = path.equals("/auth/login") || path.equals("/auth/registro");
                if (isPublic) {
                    assertThat(operation).doesNotContainKey("security");
                } else {
                    assertThat(operation.get("security").toString()).contains("bearerAuth");
                }

                @SuppressWarnings("unchecked")
                Map<String, Map<String, Object>> responses = (Map<String, Map<String, Object>>) operation.get("responses");
                if (!isPublic) {
                    assertThat(responses).containsKey("401");
                }
                responses.forEach((code, response) -> {
                    if (code.equals("204")) {
                        assertThat(response).doesNotContainKey("content");
                    } else {
                        @SuppressWarnings("unchecked")
                        Map<String, Map<String, Object>> content = (Map<String, Map<String, Object>>) response.get("content");
                        assertThat(content).isNotNull().isNotEmpty();
                        content.forEach((mediaType, media) -> {
                            assertThat(media).containsKey("schema");
                            if (code.matches("[45][0-9]{2}")) {
                                assertThat(((Map<?, ?>) media.get("schema")).get("$ref"))
                                        .as("%s %s HTTP %s", method, path, code)
                                        .isEqualTo("#/components/schemas/ErrorResponse");
                            }
                        });
                    }
                });
            }));
        });
        Set<String> documentedOperations = new HashSet<>();
        Set<String> httpMethods = Set.of("get", "post", "put", "patch", "delete", "head", "options", "trace");
        paths.forEach((path, operations) -> operations.keySet().stream().filter(httpMethods::contains)
                .forEach(method -> documentedOperations.add(method + " " + path)));
        assertThat(documentedOperations).containsExactlyInAnyOrderElementsOf(actualOperations);
    }

    @Test
    void sprintSixOperationsDocumentResponseTypesAndScopedErrorExamples() throws Exception {
        String document = document();
        String[][] contracts = {
                {"get", "/pacientes/{id}/inasistencias", "200", "InasistenciasPacienteResponse"},
                {"post", "/pacientes/{id}/inasistencias/levantamientos", "201", "InasistenciasPacienteResponse"},
                {"post", "/planes", "201", "PlanTratamientoResponse"},
                {"get", "/planes", "200", "[]PlanTratamientoResponse"},
                {"get", "/planes/{id}", "200", "PlanTratamientoResponse"},
                {"post", "/planes/{id}/citas", "201", "CitaResponse"},
                {"get", "/reportes/citas-por-doctor", "200", "ReporteCitasPorDoctorResponse"},
                {"get", "/reportes/horas-demandadas", "200", "ReporteHorasDemandadasResponse"},
                {"get", "/reportes/inasistencias", "200", "ReporteInasistenciasResponse"},
                {"get", "/reportes/ingresos-por-tratamiento", "200", "ReporteIngresosTratamientoResponse"},
                {"get", "/tratamientos", "200", "[]TratamientoResponse"},
                {"post", "/tratamientos", "201", "TratamientoResponse"},
                {"put", "/tratamientos/{id}", "200", "TratamientoResponse"},
                {"get", "/doctores/{idDoctor}/bloqueos", "200", "[]BloqueoDoctorResponse"},
                {"post", "/doctores/{idDoctor}/bloqueos", "201", "BloqueoDoctorResponse"}
        };
        for (String[] contract : contracts) {
            assertResponseSchema(document, contract[0], contract[1], contract[2], contract[3]);
        }
        Map<String, Object> errorExample = JsonPath.read(document,
                "$.paths['/reportes/citas-por-doctor'].get.responses['400'].content['application/json'].examples.fechasAusentes.value");
        assertThat(errorExample).containsEntry("status", 400).containsEntry("error", "Bad Request")
                .containsEntry("path", "/reportes/citas-por-doctor")
                .containsEntry("mensaje", "Debes indicar las fechas desde y hasta");
        Map<String, Object> conflictExample = JsonPath.read(document,
                "$.paths['/pacientes/{id}/inasistencias/levantamientos'].post.responses['409'].content['application/json'].examples.sinRestriccion.value");
        assertThat(conflictExample).containsEntry("status", 409).containsEntry("error", "Conflict")
                .containsEntry("mensaje", "El paciente no tiene la restriccion por inasistencias (0 de 3)");
    }

    @Test
    void sprintSixFiltersAndValidationLimitsAreDocumented() throws Exception {
        String document = document();
        for (String report : List.of("citas-por-doctor", "horas-demandadas", "inasistencias", "ingresos-por-tratamiento")) {
            List<Map<String, Object>> parameters = JsonPath.read(document, "$.paths['/reportes/" + report + "'].get.parameters");
            for (String name : List.of("desde", "hasta")) {
                Map<String, Object> parameter = parameters.stream().filter(p -> name.equals(p.get("name"))).findFirst().orElseThrow();
                assertThat(parameter).containsEntry("required", true).containsKey("example");
                assertThat(((Map<?, ?>) parameter.get("schema")).get("format")).isEqualTo("date");
            }
            Map<String, Object> errors = JsonPath.read(document, "$.paths['/reportes/" + report + "'].get.responses");
            assertThat(errors).containsKeys("400", "401", "403");
        }
        Map<String, Object> planErrors = JsonPath.read(document, "$.paths['/planes'].get.responses");
        assertThat(planErrors).containsKeys("400", "401", "403", "404");
        Map<String, Object> sessions = JsonPath.read(document, "$.components.schemas.PlanTratamientoRequest.properties.totalSesiones");
        assertThat(((Number) sessions.get("minimum")).intValue()).isEqualTo(1);
        assertThat(((Number) sessions.get("maximum")).intValue()).isEqualTo(50);
        Map<String, Object> reason = JsonPath.read(document, "$.components.schemas.LevantamientoInasistenciaRequest.properties.motivo");
        assertThat(reason).containsEntry("maxLength", 200).containsKey("example");
        Map<String, Object> active = JsonPath.read(document, "$.components.schemas.TratamientoRequest.properties.activo");
        assertThat(active).containsEntry("default", true).containsEntry("example", true);
        List<String> states = JsonPath.read(document, "$.components.schemas.PlanTratamientoResponse.properties.estado.enum");
        assertThat(states).containsExactlyInAnyOrder("ACTIVO", "COMPLETADO", "CANCELADO");
    }

    @Test
    void paymentDocumentationMatchesAvailableControllersAndUsesJwtIdentity() throws Exception {
        String document = document();
        Map<String, Object> paths = JsonPath.read(document, "$.paths");
        if (!paths.containsKey("/mis-planes")) {
            assertThat(paths).doesNotContainKey("/planes/{id}/abonos");
            return;
        }
        assertResponseSchema(document, "post", "/planes/{id}/abonos", "201", "AbonoPlanResponse");
        assertResponseSchema(document, "get", "/planes/{id}/abonos", "200", "AbonosPlanResponse");
        assertResponseSchema(document, "get", "/mis-planes", "200", "[]PlanTratamientoResponse");
        Map<String, Object> operation = JsonPath.read(document, "$.paths['/mis-planes'].get");
        assertThat(operation).doesNotContainKeys("parameters", "requestBody");
        Map<String, Object> properties = JsonPath.read(document, "$.components.schemas.AbonoPlanRequest.properties");
        assertThat(properties).containsOnlyKeys("monto");
        List<String> required = JsonPath.read(document, "$.components.schemas.AbonoPlanRequest.required");
        assertThat(required).containsExactly("monto");
        Map<String, Object> amount = JsonPath.read(document, "$.components.schemas.AbonoPlanRequest.properties.monto");
        assertThat(amount).containsKeys("example", "exclusiveMinimum");
        if (amount.get("exclusiveMinimum") instanceof Number exclusive) {
            assertThat(exclusive.doubleValue()).isZero();
        } else {
            assertThat(amount.get("exclusiveMinimum")).isEqualTo(true);
            assertThat(((Number) amount.get("minimum")).doubleValue()).isZero();
        }
    }

    @Test
    void examplesAndBusinessRestrictionsAreAvailableForOldAndNewDtos() throws Exception {
        String document = document();
        List<String> finalStates = JsonPath.read(document, "$.components.schemas.EstadoCitaRequest.properties.estado.enum");
        assertThat(finalStates).containsExactly("ATENDIDA", "CANCELADA", "NO_ASISTIO");
        List<String> responseStates = JsonPath.read(document, "$.components.schemas.CitaResponse.properties.estado.enum");
        assertThat(responseStates).containsExactly("AGENDADA", "ATENDIDA", "CANCELADA", "NO_ASISTIO");

        for (String dto : List.of("RegistroRequest", "CrearPersonalRequest", "HorarioRequest", "EstadoUsuarioRequest",
                "ClienteResponse", "DoctorResponse", "UsuarioListadoResponse", "ReprogramarCitaRequest",
                "BloqueoDoctorRequest", "CitaTratamientoRequest", "CitasPorDoctorResponse", "PlanTratamientoRequest",
                "SesionPlanRequest", "TratamientoRequest", "LevantamientoInasistenciaRequest", "LevantamientoInasistenciaResponse")) {
            Map<String, Map<String, Object>> properties = JsonPath.read(document, "$.components.schemas." + dto + ".properties");
            properties.forEach((name, property) -> assertThat(property).as("%s.%s", dto, name).containsKey("example"));
        }
        Map<String, Map<String, Object>> errorProperties = JsonPath.read(document, "$.components.schemas.ErrorResponse.properties");
        for (String field : List.of("status", "error", "mensaje", "path", "errores")) {
            assertThat(errorProperties.get(field)).as("ErrorResponse.%s", field).doesNotContainKey("example");
        }
        Map<String, Object> bearer = JsonPath.read(document, "$.components.securitySchemes.bearerAuth");
        assertThat(bearer).containsEntry("type", "http").containsEntry("scheme", "bearer").containsEntry("bearerFormat", "JWT");

        mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }

    @Test
    void swaggerRespectsTheForwardedApiPrefixWithoutDeploying() throws Exception {
        String document = mockMvc.perform(get("/v3/api-docs")
                        .header("X-Forwarded-Prefix", "/api")
                        .header("X-Forwarded-Host", "clinica.example")
                        .header("X-Forwarded-Proto", "https"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String server = JsonPath.read(document, "$.servers[0].url");
        assertThat(server).isEqualTo("https://clinica.example/api");
        String redirect = mockMvc.perform(get("/swagger-ui.html").header("X-Forwarded-Prefix", "/api"))
                .andExpect(status().is3xxRedirection()).andReturn().getResponse().getHeader("Location");
        assertThat(redirect).endsWith("/api/swagger-ui/index.html");
        mockMvc.perform(get("/v3/api-docs/swagger-config").header("X-Forwarded-Prefix", "/api"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("/api/v3/api-docs"));
    }

    private void assertResponseSchema(String document, String method, String path, String code, String expected) {
        Map<String, Map<String, Object>> content = JsonPath.read(document,
                "$.paths['" + path + "']." + method + ".responses['" + code + "'].content");
        assertThat(content).as("%s %s HTTP %s", method, path, code).isNotEmpty();
        content.values().forEach(media -> {
            Map<?, ?> schema = (Map<?, ?>) media.get("schema");
            if (expected.startsWith("[]")) {
                assertThat(schema.get("type")).isEqualTo("array");
                schema = (Map<?, ?>) schema.get("items");
            }
            assertThat(schema.get("$ref")).isEqualTo("#/components/schemas/" + expected.replace("[]", ""));
        });
    }

    private String document() throws Exception {
        return mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }
}
