package com.teo1.clinicadental.controller;

import com.teo1.clinicadental.dto.VersionResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class VersionControllerTests {

    @Test
    void recortaElShaCompletoASieteCaracteres() {
        VersionResponse version = obtener("dc457805204d752a8b8f17e0bb2993ac1bea0579", "2026-10-28T16:22:12Z");

        assertThat(version.getCommit()).isEqualTo("dc45780");
        assertThat(version.getFechaConstruccion()).isEqualTo(Instant.parse("2026-10-28T16:22:12Z"));
    }

    @Test
    void unHashYaCortoQuedaIgual() {
        assertThat(obtener(" dc45780 ", "").getCommit()).isEqualTo("dc45780");
    }

    @Test
    void sinDatosDelBuildDevuelveNull() {
        VersionResponse version = obtener("", "  ");

        assertThat(version.getCommit()).isNull();
        assertThat(version.getFechaConstruccion()).isNull();
    }

    @Test
    void unaFechaMalFormadaSeIgnora() {
        VersionResponse version = obtener("dc45780", "28/10/2026");

        assertThat(version.getCommit()).isEqualTo("dc45780");
        assertThat(version.getFechaConstruccion()).isNull();
    }

    private static VersionResponse obtener(String commit, String fecha) {
        return new VersionController(commit, fecha).obtener().getBody();
    }
}
