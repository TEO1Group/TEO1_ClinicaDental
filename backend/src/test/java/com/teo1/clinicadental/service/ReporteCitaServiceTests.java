package com.teo1.clinicadental.service;

import com.teo1.clinicadental.dto.CitasPorDoctorResponse;
import com.teo1.clinicadental.dto.ReporteCitasPorDoctorResponse;
import com.teo1.clinicadental.model.Doctor;
import com.teo1.clinicadental.model.EstadoCita;
import com.teo1.clinicadental.model.EstadoUsuario;
import com.teo1.clinicadental.model.Usuario;
import com.teo1.clinicadental.repository.DoctorRepository;
import com.teo1.clinicadental.repository.ReporteCitaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReporteCitaServiceTests {

    private static final LocalDate DESDE = LocalDate.of(2026, 10, 1);
    private static final LocalDate HASTA = LocalDate.of(2026, 10, 31);

    @Mock
    private ReporteCitaRepository reporteCitaRepository;

    @Mock
    private DoctorRepository doctorRepository;

    private ReporteCitaService reporteCitaService;
    private Doctor perez;
    private Doctor diaz;
    private Doctor sinCitas;

    @BeforeEach
    void setUp() {
        reporteCitaService = new ReporteCitaService(reporteCitaRepository, doctorRepository);
        perez = doctor("Carlos", "Perez", "Ortodoncia");
        diaz = doctor("Laura", "Diaz", "Endodoncia");
        sinCitas = doctor("Ana", "Diaz", "Odontologia general");
    }

    @Test
    void cuentaLasCitasPorEstadoYElTotal() {
        when(doctorRepository.findByUsuarioEstado(EstadoUsuario.ACTIVO)).thenReturn(List.of(perez, diaz));
        when(reporteCitaRepository.contarPorDoctorYEstado(DESDE, HASTA)).thenReturn(List.of(
                conteo(perez, EstadoCita.AGENDADA, 4),
                conteo(perez, EstadoCita.ATENDIDA, 10),
                conteo(perez, EstadoCita.CANCELADA, 2),
                conteo(perez, EstadoCita.NO_ASISTIO, 1),
                conteo(diaz, EstadoCita.ATENDIDA, 3)));

        ReporteCitasPorDoctorResponse reporte = reporteCitaService.citasPorDoctor(DESDE, HASTA);

        assertEquals(DESDE, reporte.getDesde());
        assertEquals(HASTA, reporte.getHasta());
        CitasPorDoctorResponse filaPerez = fila(reporte, perez);
        assertEquals(4, filaPerez.getAgendadas());
        assertEquals(10, filaPerez.getAtendidas());
        assertEquals(2, filaPerez.getCanceladas());
        assertEquals(1, filaPerez.getNoAsistio());
        assertEquals(17, filaPerez.getTotal());
        assertEquals("Carlos", filaPerez.getNombreDoctor());
        assertEquals("Perez", filaPerez.getApellidoDoctor());
        assertEquals("Ortodoncia", filaPerez.getEspecialidad());

        CitasPorDoctorResponse filaDiaz = fila(reporte, diaz);
        assertEquals(0, filaDiaz.getAgendadas());
        assertEquals(3, filaDiaz.getAtendidas());
        assertEquals(3, filaDiaz.getTotal());
    }

    @Test
    void doctorSinCitasApareceConCeros() {
        when(doctorRepository.findByUsuarioEstado(EstadoUsuario.ACTIVO)).thenReturn(List.of(sinCitas));
        when(reporteCitaRepository.contarPorDoctorYEstado(DESDE, HASTA)).thenReturn(List.of());

        CitasPorDoctorResponse fila = fila(reporteCitaService.citasPorDoctor(DESDE, HASTA), sinCitas);

        assertEquals(0, fila.getAgendadas());
        assertEquals(0, fila.getAtendidas());
        assertEquals(0, fila.getCanceladas());
        assertEquals(0, fila.getNoAsistio());
        assertEquals(0, fila.getTotal());
    }

    @Test
    void conteosDeDoctoresInactivosNoAparecen() {
        Doctor inactivo = doctor("Pedro", "Gomez", "Ortodoncia");
        when(doctorRepository.findByUsuarioEstado(EstadoUsuario.ACTIVO)).thenReturn(List.of(perez));
        when(reporteCitaRepository.contarPorDoctorYEstado(DESDE, HASTA)).thenReturn(List.of(
                conteo(inactivo, EstadoCita.ATENDIDA, 5)));

        ReporteCitasPorDoctorResponse reporte = reporteCitaService.citasPorDoctor(DESDE, HASTA);

        assertEquals(1, reporte.getDoctores().size());
        assertEquals(perez.getId(), reporte.getDoctores().get(0).getIdDoctor());
        assertEquals(0, reporte.getDoctores().get(0).getTotal());
    }

    @Test
    void doctoresOrdenadosPorApellidoYNombre() {
        when(doctorRepository.findByUsuarioEstado(EstadoUsuario.ACTIVO)).thenReturn(List.of(perez, diaz, sinCitas));
        when(reporteCitaRepository.contarPorDoctorYEstado(DESDE, HASTA)).thenReturn(List.of());

        List<CitasPorDoctorResponse> doctores = reporteCitaService.citasPorDoctor(DESDE, HASTA).getDoctores();

        assertEquals(List.of(sinCitas.getId(), diaz.getId(), perez.getId()),
                doctores.stream().map(CitasPorDoctorResponse::getIdDoctor).toList());
    }

    @Test
    void unSoloDiaEsUnPeriodoValido() {
        when(doctorRepository.findByUsuarioEstado(EstadoUsuario.ACTIVO)).thenReturn(List.of());
        when(reporteCitaRepository.contarPorDoctorYEstado(DESDE, DESDE)).thenReturn(List.of());

        assertEquals(0, reporteCitaService.citasPorDoctor(DESDE, DESDE).getDoctores().size());
    }

    @Test
    void parametrosFaltantesDevuelvenSolicitudInvalida() {
        for (LocalDate[] fechas : new LocalDate[][] {{null, HASTA}, {DESDE, null}, {null, null}}) {
            ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                    () -> reporteCitaService.citasPorDoctor(fechas[0], fechas[1]));
            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
            assertEquals("Debes indicar las fechas desde y hasta", exception.getReason());
        }
        verifyNoInteractions(reporteCitaRepository, doctorRepository);
    }

    @Test
    void desdePosteriorAHastaDevuelveSolicitudInvalida() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> reporteCitaService.citasPorDoctor(HASTA, DESDE));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("La fecha desde no puede ser posterior a hasta", exception.getReason());
        verifyNoInteractions(reporteCitaRepository, doctorRepository);
    }

    private CitasPorDoctorResponse fila(ReporteCitasPorDoctorResponse reporte, Doctor doctor) {
        return reporte.getDoctores().stream()
                .filter(fila -> fila.getIdDoctor().equals(doctor.getId()))
                .findFirst()
                .orElseThrow();
    }

    private Doctor doctor(String nombre, String apellido, String especialidad) {
        return Doctor.builder()
                .id(UUID.randomUUID())
                .usuario(Usuario.builder().id(UUID.randomUUID()).nombre(nombre).apellido(apellido).build())
                .especialidad(especialidad)
                .build();
    }

    private ReporteCitaRepository.ConteoCitas conteo(Doctor doctor, EstadoCita estado, long cantidad) {
        return new ReporteCitaRepository.ConteoCitas() {
            @Override
            public UUID getIdDoctor() {
                return doctor.getId();
            }

            @Override
            public EstadoCita getEstado() {
                return estado;
            }

            @Override
            public long getCantidad() {
                return cantidad;
            }
        };
    }
}
