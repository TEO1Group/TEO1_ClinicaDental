package com.teo1.clinicadental.service;

import com.teo1.clinicadental.model.Cliente;
import com.teo1.clinicadental.model.EstadoCita;
import com.teo1.clinicadental.repository.CitaRepository;
import com.teo1.clinicadental.repository.ClienteRepository;
import com.teo1.clinicadental.repository.LevantamientoInasistenciaRepository;
import com.teo1.clinicadental.repository.UsuarioRepository;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InasistenciaServiceTests {

    @Mock
    private LevantamientoInasistenciaRepository levantamientoRepository;

    @Mock
    private CitaRepository citaRepository;

    @Mock
    private ClienteRepository clienteRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    private final Cliente cliente = Cliente.builder().id(UUID.randomUUID()).build();

    @Test
    void elLimiteSeTomaDeLaConfiguracion() {
        when(levantamientoRepository.findFirstByClienteIdOrderByFechaDesc(cliente.getId())).thenReturn(Optional.empty());
        when(citaRepository.countByClienteIdAndEstado(cliente.getId(), EstadoCita.NO_ASISTIO)).thenReturn(2L);

        assertDoesNotThrow(() -> servicio(3).validarPuedeAgendar(cliente));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> servicio(2).validarPuedeAgendar(cliente));
        assertEquals(HttpStatus.CONFLICT, error.getStatusCode());
    }

    private InasistenciaService servicio(int maxInasistencias) {
        return new InasistenciaService(levantamientoRepository, citaRepository, clienteRepository,
                usuarioRepository, Clock.systemDefaultZone(), maxInasistencias);
    }
}
