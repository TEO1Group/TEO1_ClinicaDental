package com.teo1.clinicadental.service;

import com.teo1.clinicadental.model.Cita;
import com.teo1.clinicadental.model.EstadoCita;
import com.teo1.clinicadental.model.Usuario;
import com.teo1.clinicadental.repository.CitaRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Envia una vez al dia el correo de recordatorio de las citas AGENDADA de las proximas 24 horas
 * y las marca con recordatorio_enviado para no repetirlo. Si SMTP_HOST o SMTP_FROM estan
 * vacios no hace nada, asi el CI y el desarrollo local no necesitan servidor de correo.
 */
@Slf4j
@Service
public class RecordatorioCitaService {

    private static final int HORAS_ANTICIPACION = 24;
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final CitaRepository citaRepository;
    private final ObjectProvider<JavaMailSender> mailSender;
    private final Clock clock;
    private final String smtpHost;
    private final String remitente;

    public RecordatorioCitaService(
            CitaRepository citaRepository,
            ObjectProvider<JavaMailSender> mailSender,
            Clock clock,
            @Value("${spring.mail.host:}") String smtpHost,
            @Value("${app.mail.remitente:}") String remitente
    ) {
        this.citaRepository = citaRepository;
        this.mailSender = mailSender;
        this.clock = clock;
        this.smtpHost = smtpHost;
        this.remitente = remitente;
    }

    @Scheduled(cron = "${app.citas.recordatorio-cron:0 0 7 * * *}", zone = "${app.zona-horaria}")
    public void enviarRecordatorios() {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (smtpHost.isBlank() || remitente.isBlank() || sender == null) {
            return;
        }

        LocalDateTime ahora = LocalDateTime.now(clock);
        LocalDateTime limite = ahora.plusHours(HORAS_ANTICIPACION);

        int enviados = 0;
        for (Cita cita : citaRepository.findByEstadoAndRecordatorioEnviadoFalseAndFechaBetweenOrderByFechaAscHoraAsc(
                EstadoCita.AGENDADA, ahora.toLocalDate(), limite.toLocalDate())) {
            LocalDateTime inicio = LocalDateTime.of(cita.getFecha(), cita.getHora());
            if (inicio.isBefore(ahora) || inicio.isAfter(limite)) {
                continue;
            }

            try {
                sender.send(mensaje(cita));
                cita.setRecordatorioEnviado(true);
                citaRepository.save(cita);
                enviados++;
            } catch (MailException exception) {
                // Queda sin marcar para intentarlo en la siguiente ejecucion
                log.warn("No se pudo enviar el recordatorio de la cita {}: {}", cita.getId(), exception.getMessage());
            }
        }

        if (enviados > 0) {
            log.info("Recordatorios de cita enviados: {}", enviados);
        }
    }

    private SimpleMailMessage mensaje(Cita cita) {
        Usuario paciente = cita.getCliente().getUsuario();
        Usuario doctor = cita.getDoctor().getUsuario();

        SimpleMailMessage mensaje = new SimpleMailMessage();
        mensaje.setFrom(remitente);
        mensaje.setTo(paciente.getEmail());
        mensaje.setSubject("Recordatorio de tu cita en la Clinica Dental");
        mensaje.setText(String.format(
                "Hola %s,%n%nTe recordamos tu cita con %s %s el %s a las %s.%n%n"
                        + "Si no puedes asistir, cancela la cita desde la aplicacion.%n%nClinica Dental",
                paciente.getNombre(),
                doctor.getNombre(),
                doctor.getApellido(),
                cita.getFecha().format(FORMATO_FECHA),
                cita.getHora().format(FORMATO_HORA)
        ));
        return mensaje;
    }
}
