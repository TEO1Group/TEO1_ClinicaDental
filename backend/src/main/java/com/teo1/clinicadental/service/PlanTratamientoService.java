package com.teo1.clinicadental.service;

import com.teo1.clinicadental.dto.CitaRequest;
import com.teo1.clinicadental.dto.CitaResponse;
import com.teo1.clinicadental.dto.PlanTratamientoRequest;
import com.teo1.clinicadental.dto.PlanTratamientoResponse;
import com.teo1.clinicadental.dto.SesionPlanRequest;
import com.teo1.clinicadental.model.Cliente;
import com.teo1.clinicadental.model.Doctor;
import com.teo1.clinicadental.model.EstadoCita;
import com.teo1.clinicadental.model.EstadoPlan;
import com.teo1.clinicadental.model.EstadoUsuario;
import com.teo1.clinicadental.model.PlanTratamiento;
import com.teo1.clinicadental.model.Rol;
import com.teo1.clinicadental.model.Tratamiento;
import com.teo1.clinicadental.repository.AbonoPlanRepository;
import com.teo1.clinicadental.repository.CitaRepository;
import com.teo1.clinicadental.repository.ClienteRepository;
import com.teo1.clinicadental.repository.DoctorRepository;
import com.teo1.clinicadental.repository.PlanTratamientoRepository;
import com.teo1.clinicadental.repository.TratamientoRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Planes de tratamiento (HU-26). Las sesiones son citas creadas con {@link CitaService#crearCita},
 * asi respetan las mismas reglas de agendado (horario, bloqueos del doctor, inasistencias), y se
 * vinculan al plan con {@code cita.plan}.
 * <p>
 * Una sesion ocupa un lugar del plan mientras su cita esta AGENDADA o ATENDIDA; si se cancela o el
 * paciente no asiste, se puede agendar otra. El avance es atendidas sobre totales.
 */
@Service
public class PlanTratamientoService {

    private final PlanTratamientoRepository planRepository;
    private final AbonoPlanRepository abonoPlanRepository;
    private final CitaRepository citaRepository;
    private final ClienteRepository clienteRepository;
    private final DoctorRepository doctorRepository;
    private final TratamientoRepository tratamientoRepository;
    private final CitaService citaService;
    private final CitaMapper citaMapper;
    private final Clock clock;

    public PlanTratamientoService(
            PlanTratamientoRepository planRepository,
            AbonoPlanRepository abonoPlanRepository,
            CitaRepository citaRepository,
            ClienteRepository clienteRepository,
            DoctorRepository doctorRepository,
            TratamientoRepository tratamientoRepository,
            CitaService citaService,
            CitaMapper citaMapper,
            Clock clock
    ) {
        this.planRepository = planRepository;
        this.abonoPlanRepository = abonoPlanRepository;
        this.citaRepository = citaRepository;
        this.clienteRepository = clienteRepository;
        this.doctorRepository = doctorRepository;
        this.tratamientoRepository = tratamientoRepository;
        this.citaService = citaService;
        this.citaMapper = citaMapper;
        this.clock = clock;
    }

    @Transactional
    public PlanTratamientoResponse crearPlan(PlanTratamientoRequest request, Authentication authentication) {
        Doctor doctor = resolverDoctor(request.getIdDoctor(), authentication);
        if (doctor.getUsuario().getEstado() != EstadoUsuario.ACTIVO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El doctor no esta activo");
        }

        Cliente cliente = clienteRepository.findById(request.getIdCliente())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Paciente no encontrado"));
        if (cliente.getUsuario().getEstado() != EstadoUsuario.ACTIVO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El paciente no esta activo");
        }

        Tratamiento tratamiento = tratamientoRepository.findById(request.getIdTratamiento())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tratamiento no encontrado"));
        if (!tratamiento.isActivo()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El tratamiento no esta activo en el catalogo");
        }

        BigDecimal costoTotal = request.getCostoTotal() != null
                ? request.getCostoTotal()
                : tratamiento.getCosto().multiply(BigDecimal.valueOf(request.getTotalSesiones()));

        PlanTratamiento plan = planRepository.save(PlanTratamiento.builder()
                .cliente(cliente)
                .doctor(doctor)
                .tratamiento(tratamiento)
                .totalSesiones(request.getTotalSesiones())
                .costoTotal(costoTotal)
                .estado(EstadoPlan.ACTIVO)
                .fechaCreacion(LocalDateTime.now(clock))
                .build());

        return resumir(List.of(plan)).get(0);
    }

    @Transactional(readOnly = true)
    public List<PlanTratamientoResponse> listarPlanes(
            UUID idCliente, UUID idDoctor, EstadoPlan estado, Authentication authentication) {
        List<PlanTratamiento> planes = tieneRol(authentication, Rol.DOCTOR)
                ? planRepository.findByDoctorIdOrderByFechaCreacionDesc(doctorDelUsuario(authentication).getId())
                : planRepository.findAllByOrderByFechaCreacionDesc();

        return resumir(planes.stream()
                .filter(plan -> idCliente == null || plan.getCliente().getId().equals(idCliente))
                .filter(plan -> idDoctor == null || plan.getDoctor().getId().equals(idDoctor))
                .toList())
                .stream()
                .filter(plan -> estado == null || plan.getEstado() == estado)
                .toList();
    }

    @Transactional(readOnly = true)
    public PlanTratamientoResponse obtenerPlan(UUID id, Authentication authentication) {
        PlanTratamiento plan = planRepository.findConRelacionesById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan no encontrado"));
        validarAcceso(plan, authentication);

        List<CitaResponse> citas = citaRepository.findByPlanIdOrderByFechaAscHoraAsc(id).stream()
                .map(citaMapper::toResponse)
                .toList();
        return resumir(List.of(plan), citas).get(0);
    }

    @Transactional
    public CitaResponse agendarSesion(UUID id, SesionPlanRequest request, Authentication authentication) {
        PlanTratamiento plan = planRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan no encontrado"));

        PlanTratamientoResponse resumen = resumir(List.of(plan)).get(0);
        if (resumen.getEstado() == EstadoPlan.CANCELADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El plan esta cancelado");
        }
        if (resumen.getEstado() == EstadoPlan.COMPLETADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El plan ya esta completado");
        }
        if (resumen.getSesionesPendientes() == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ya se agendaron las " + plan.getTotalSesiones() + " sesiones del plan");
        }

        CitaRequest citaRequest = new CitaRequest();
        citaRequest.setIdCliente(plan.getCliente().getId());
        citaRequest.setIdDoctor(plan.getDoctor().getId());
        citaRequest.setFecha(request.getFecha());
        citaRequest.setHora(request.getHora());
        citaRequest.setNotas(request.getNotas() != null && !request.getNotas().isBlank()
                ? request.getNotas()
                : "Sesion " + (resumen.getSesionesAtendidas() + resumen.getSesionesAgendadas() + 1)
                        + " de " + plan.getTotalSesiones() + ": " + plan.getTratamiento().getNombre());

        CitaResponse cita = citaService.crearCita(citaRequest, authentication);
        citaRepository.findById(cita.getIdCita()).orElseThrow().setPlan(plan);
        return cita;
    }

    private List<PlanTratamientoResponse> resumir(List<PlanTratamiento> planes) {
        return resumir(planes, null);
    }

    // Calcula avance y saldo de todos los planes con dos consultas agrupadas
    private List<PlanTratamientoResponse> resumir(List<PlanTratamiento> planes, List<CitaResponse> citas) {
        if (planes.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = planes.stream().map(PlanTratamiento::getId).toList();

        Map<UUID, Map<EstadoCita, Long>> sesiones = new HashMap<>();
        planRepository.contarSesiones(ids).forEach(conteo -> sesiones
                .computeIfAbsent(conteo.getIdPlan(), plan -> new HashMap<>())
                .put(conteo.getEstado(), conteo.getCantidad()));

        Map<UUID, BigDecimal> abonado = new HashMap<>();
        abonoPlanRepository.sumarPorPlan(ids).forEach(total -> abonado.put(total.getIdPlan(), total.getTotal()));

        return planes.stream().map(plan -> {
            Map<EstadoCita, Long> porEstado = sesiones.getOrDefault(plan.getId(), Map.of());
            long atendidas = porEstado.getOrDefault(EstadoCita.ATENDIDA, 0L);
            long agendadas = porEstado.getOrDefault(EstadoCita.AGENDADA, 0L);
            long pendientes = Math.max(0, plan.getTotalSesiones() - atendidas - agendadas);
            BigDecimal totalAbonado = abonado.getOrDefault(plan.getId(), BigDecimal.ZERO);

            EstadoPlan estado = plan.getEstado();
            if (estado == EstadoPlan.ACTIVO && atendidas >= plan.getTotalSesiones()) {
                estado = EstadoPlan.COMPLETADO;
            }

            return new PlanTratamientoResponse(
                    plan.getId(),
                    plan.getCliente().getId(),
                    plan.getCliente().getUsuario().getNombre(),
                    plan.getCliente().getUsuario().getApellido(),
                    plan.getDoctor().getId(),
                    plan.getDoctor().getUsuario().getNombre(),
                    plan.getDoctor().getUsuario().getApellido(),
                    plan.getTratamiento().getId(),
                    plan.getTratamiento().getNombre(),
                    estado,
                    plan.getFechaCreacion(),
                    plan.getTotalSesiones(),
                    atendidas,
                    agendadas,
                    pendientes,
                    (int) Math.min(100, atendidas * 100 / plan.getTotalSesiones()),
                    plan.getCostoTotal(),
                    totalAbonado,
                    plan.getCostoTotal().subtract(totalAbonado),
                    citas
            );
        }).toList();
    }

    // Un DOCTOR crea planes solo para si mismo; ADMIN y SECRETARIA indican el doctor
    private Doctor resolverDoctor(UUID idDoctor, Authentication authentication) {
        if (tieneRol(authentication, Rol.DOCTOR)) {
            Doctor propio = doctorDelUsuario(authentication);
            if (idDoctor != null && !idDoctor.equals(propio.getId())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo puedes crear planes para ti mismo");
            }
            return propio;
        }
        if (idDoctor == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debes indicar el doctor del plan");
        }
        return doctorRepository.findById(idDoctor)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Doctor no encontrado"));
    }

    private void validarAcceso(PlanTratamiento plan, Authentication authentication) {
        if (tieneRol(authentication, Rol.DOCTOR)
                && !plan.getDoctor().getUsuario().getId().toString().equals(authentication.getName())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Solo puedes ver tus propios planes");
        }
    }

    private Doctor doctorDelUsuario(Authentication authentication) {
        return doctorRepository.findByUsuarioId(UUID.fromString(authentication.getName()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tu usuario no tiene un doctor asociado"));
    }

    private boolean tieneRol(Authentication authentication, Rol rol) {
        String rolBuscado = "ROLE_" + rol.name();
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> rolBuscado.equals(authority.getAuthority()));
    }
}
