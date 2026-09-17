package com.teo1.clinicadental.service;

import com.teo1.clinicadental.dto.CrearPersonalRequest;
import com.teo1.clinicadental.dto.RegistroResponse;
import com.teo1.clinicadental.dto.RolResponse;
import com.teo1.clinicadental.dto.UsuarioListadoResponse;
import com.teo1.clinicadental.model.Doctor;
import com.teo1.clinicadental.model.EstadoUsuario;
import com.teo1.clinicadental.model.Rol;
import com.teo1.clinicadental.model.RolEntity;
import com.teo1.clinicadental.model.Secretaria;
import com.teo1.clinicadental.model.Turno;
import com.teo1.clinicadental.model.Usuario;
import com.teo1.clinicadental.repository.DoctorRepository;
import com.teo1.clinicadental.repository.RolRepository;
import com.teo1.clinicadental.repository.SecretariaRepository;
import com.teo1.clinicadental.repository.UsuarioRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AdminUsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final DoctorRepository doctorRepository;
    private final SecretariaRepository secretariaRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<UsuarioListadoResponse> listarUsuarios() {
        return usuarioRepository.findAll().stream()
                .map(usuario -> new UsuarioListadoResponse(
                        usuario.getId(),
                        usuario.getNombre(),
                        usuario.getApellido(),
                        usuario.getEmail(),
                        usuario.getTelefono(),
                        usuario.getRol().getNombreRol().name(),
                        usuario.getEstado().name(),
                        usuario.getFechaCreacion()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RolResponse> listarRoles() {
        return rolRepository.findAll().stream()
                .map(rol -> new RolResponse(rol.getIdRol(), rol.getNombreRol().name(), rol.getDescripcion()))
                .toList();
    }

    @Transactional
    public RegistroResponse crearPersonal(CrearPersonalRequest request) {
        Rol rol = parseRol(request.getIdRol());

        if (rol == Rol.CLIENTE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Los clientes se registran en /auth/registro");
        }

        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El email ya esta registrado");
        }

        if (rol == Rol.DOCTOR && (request.getEspecialidad() == null || request.getEspecialidad().isBlank())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La especialidad es obligatoria para un doctor");
        }

        RolEntity rolEntity = rolRepository.findByNombreRol(rol)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "El rol " + rol.name() + " no esta configurado en la base de datos"
                ));

        Usuario usuario = Usuario.builder()
                .nombre(request.getNombre())
                .apellido(request.getApellido())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .telefono(request.getTelefono())
                .rol(rolEntity)
                .estado(EstadoUsuario.ACTIVO)
                .build();

        Usuario usuarioGuardado = usuarioRepository.save(usuario);

        if (rol == Rol.DOCTOR) {
            Doctor doctor = Doctor.builder()
                    .usuario(usuarioGuardado)
                    .especialidad(request.getEspecialidad())
                    .numeroColegiado(request.getNumeroColegiado())
                    .numeroClinica(request.getNumeroClinica())
                    .build();
            doctorRepository.save(doctor);
        } else if (rol == Rol.SECRETARIA) {
            Secretaria secretaria = Secretaria.builder()
                    .usuario(usuarioGuardado)
                    .turno(parseTurno(request.getTurno()))
                    .area(request.getArea() != null && !request.getArea().isBlank() ? request.getArea() : "RECEPCION")
                    .build();
            secretariaRepository.save(secretaria);
        }

        return new RegistroResponse("Usuario registrado exitosamente", usuarioGuardado.getId());
    }

    private Rol parseRol(String idRol) {
        try {
            return Rol.valueOf(idRol);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Rol invalido: " + idRol);
        }
    }

    private Turno parseTurno(String turno) {
        if (turno == null || turno.isBlank()) {
            return null;
        }
        try {
            return Turno.valueOf(turno);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Turno invalido: " + turno);
        }
    }
}
