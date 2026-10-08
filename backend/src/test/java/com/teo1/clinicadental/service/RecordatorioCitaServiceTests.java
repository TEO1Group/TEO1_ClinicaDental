package com.teo1.clinicadental.service;

import com.teo1.clinicadental.model.Cita;
import com.teo1.clinicadental.model.Cliente;
import com.teo1.clinicadental.model.Doctor;
import com.teo1.clinicadental.model.EstadoCita;
import com.teo1.clinicadental.model.EstadoUsuario;
import com.teo1.clinicadental.model.Usuario;
import com.teo1.clinicadental.repository.CitaRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecordatorioCitaServiceTests {

    private static final ZoneId ZONA = ZoneId.of("America/Guatemala");
    private static final LocalDate HOY = LocalDate.of(2026, 10, 8);
    private static final LocalDate MANANA = HOY.plusDays(1);

    @Mock
    private CitaRepository citaRepository;

    @Mock
    private ObjectProvider<JavaMailSender> mailSenderProvider;

    @Mock
    private JavaMailSender mailSender;

    private Clock clock;

    @BeforeEach
    void setUp() {
        // Las pruebas corren a las 08:00 del 2026-10-08
        clock = Clock.fixed(ZonedDateTime.of(2026, 10, 8, 8, 0, 0, 0, ZONA).toInstant(), ZONA);
    }

    @Test
    void sinSmtpConfiguradoNoHaceNada() {
        servicio("", "").enviarRecordatorios();

        verifyNoInteractions(citaRepository, mailSender);
    }

    @Test
    void enviaCorreoSoloALasCitasDeLasProximas24HorasYLasMarca() {
        Cita en10Horas = cita(HOY, LocalTime.of(18, 0));
        Cita en30Horas = cita(MANANA, LocalTime.of(14, 0));
        prepararCitas(List.of(en10Horas, en30Horas));

        servicio("smtp.prueba.com", "clinica@prueba.com").enviarRecordatorios();

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertArrayEquals(new String[] {"ana@prueba.com"}, captor.getValue().getTo());
        assertTrue(captor.getValue().getText().contains("08/10/2026 a las 18:00"));
        assertTrue(en10Horas.isRecordatorioEnviado());
        assertFalse(en30Horas.isRecordatorioEnviado());
        verify(citaRepository).save(en10Horas);
    }

    @Test
    void siFallaElEnvioLaCitaQuedaSinMarcar() {
        Cita cita = cita(HOY, LocalTime.of(18, 0));
        prepararCitas(List.of(cita));
        doThrow(new MailSendException("SMTP caido")).when(mailSender).send(any(SimpleMailMessage.class));

        servicio("smtp.prueba.com", "clinica@prueba.com").enviarRecordatorios();

        assertFalse(cita.isRecordatorioEnviado());
        verify(citaRepository, never()).save(any());
    }

    private RecordatorioCitaService servicio(String host, String remitente) {
        return new RecordatorioCitaService(citaRepository, mailSenderProvider, clock, host, remitente);
    }

    private void prepararCitas(List<Cita> citas) {
        when(mailSenderProvider.getIfAvailable()).thenReturn(mailSender);
        when(citaRepository.findByEstadoAndRecordatorioEnviadoFalseAndFechaBetweenOrderByFechaAscHoraAsc(
                EstadoCita.AGENDADA, HOY, MANANA)).thenReturn(citas);
    }

    private Cita cita(LocalDate fecha, LocalTime hora) {
        return Cita.builder()
                .id(UUID.randomUUID())
                .cliente(Cliente.builder().id(UUID.randomUUID()).usuario(usuario("Ana", "ana@prueba.com")).dpi("1234567890101").build())
                .doctor(Doctor.builder().id(UUID.randomUUID()).usuario(usuario("Carlos", "carlos@prueba.com")).especialidad("General").build())
                .fecha(fecha)
                .hora(hora)
                .estado(EstadoCita.AGENDADA)
                .build();
    }

    private Usuario usuario(String nombre, String email) {
        return Usuario.builder()
                .id(UUID.randomUUID())
                .nombre(nombre)
                .apellido("Prueba")
                .email(email)
                .passwordHash("hash")
                .estado(EstadoUsuario.ACTIVO)
                .build();
    }
}
