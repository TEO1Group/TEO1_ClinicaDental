package com.teo1.clinicadental.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * GET /version con los valores que el CD pasa al build de la imagen: el SHA completo y la fecha en UTC.
 */
@SpringBootTest(properties = {
        "app.version.commit=dc457805204d752a8b8f17e0bb2993ac1bea0579",
        "app.version.fecha-construccion=2026-10-28T16:22:12Z"
})
@AutoConfigureMockMvc
class VersionIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void respondeSinTokenConElHashCortoYLaFecha() throws Exception {
        mockMvc.perform(get("/version"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commit").value("dc45780"))
                .andExpect(jsonPath("$.fechaConstruccion").value("2026-10-28T16:22:12Z"));
    }

    @Test
    void unTokenInvalidoNoImpideConsultarla() throws Exception {
        mockMvc.perform(get("/version").header("Authorization", "Bearer token-vencido"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commit").value("dc45780"));
    }

    @Test
    void soloElGetEsPublico() throws Exception {
        mockMvc.perform(post("/version")).andExpect(status().isUnauthorized());
    }

    @Test
    void swaggerLaDocumentaComoPublica() throws Exception {
        String document = mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        Map<String, Object> operation = JsonPath.read(document, "$.paths['/version'].get");
        assertThat(operation).doesNotContainKey("security");
        assertThat(JsonPath.<String>read(document,
                "$.paths['/version'].get.responses['200'].content['*/*'].schema['$ref']"))
                .isEqualTo("#/components/schemas/VersionResponse");
        assertThat(JsonPath.<Map<String, Object>>read(document, "$.components.schemas.VersionResponse.properties"))
                .containsOnlyKeys("commit", "fechaConstruccion");
    }
}
