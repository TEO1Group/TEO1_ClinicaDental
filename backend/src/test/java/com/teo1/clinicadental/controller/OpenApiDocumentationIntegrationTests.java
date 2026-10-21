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
                        content.forEach((mediaType, media) -> assertThat(media).containsKey("schema"));
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
    void reportsAndDoctorBlocksDocumentTheirResponseTypesAndScopedErrorExample() throws Exception {
        String document = document();
        Map<String, Map<String, Object>> reportContent = JsonPath.read(document,
                "$.paths['/reportes/citas-por-doctor'].get.responses['200'].content");
        reportContent.values().forEach(media -> assertThat(((Map<?, ?>) media.get("schema")).get("$ref"))
                .isEqualTo("#/components/schemas/ReporteCitasPorDoctorResponse"));
        Map<String, Object> errorExample = JsonPath.read(document,
                "$.paths['/reportes/citas-por-doctor'].get.responses['400'].content['application/json'].examples.fechasAusentes.value");
        assertThat(errorExample).containsEntry("status", 400).containsEntry("error", "Bad Request")
                .containsEntry("path", "/reportes/citas-por-doctor")
                .containsEntry("mensaje", "Debes indicar las fechas desde y hasta");
        Map<String, Map<String, Object>> blocksContent = JsonPath.read(document,
                "$.paths['/doctores/{idDoctor}/bloqueos'].get.responses['200'].content");
        blocksContent.values().forEach(media -> {
            Map<?, ?> schema = (Map<?, ?>) media.get("schema");
            assertThat(schema.get("type")).isEqualTo("array");
            assertThat(((Map<?, ?>) schema.get("items")).get("$ref")).isEqualTo("#/components/schemas/BloqueoDoctorResponse");
        });
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
                "BloqueoDoctorRequest", "CitaTratamientoRequest", "CitasPorDoctorResponse")) {
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

    private String document() throws Exception {
        return mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }
}
