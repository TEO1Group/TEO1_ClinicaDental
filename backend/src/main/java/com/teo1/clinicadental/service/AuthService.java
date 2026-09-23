package com.teo1.clinicadental.service;

import com.teo1.clinicadental.dto.LoginRequest;
import com.teo1.clinicadental.dto.LoginResponse;
import com.teo1.clinicadental.dto.RegistroRequest;
import com.teo1.clinicadental.dto.RegistroResponse;
import com.teo1.clinicadental.dto.UsuarioActualResponse;
import com.teo1.clinicadental.model.Cliente;
import com.teo1.clinicadental.model.Doctor;
import com.teo1.clinicadental.model.EstadoUsuario;
import com.teo1.clinicadental.model.Rol;
import com.teo1.clinicadental.model.RolEntity;
import com.teo1.clinicadental.model.Secretaria;
import com.teo1.clinicadental.model.Usuario;
import com.teo1.clinicadental.repository.ClienteRepository;
import com.teo1.clinicadental.repository.DoctorRepository;
import com.teo1.clinicadental.repository.RolRepository;
import com.teo1.clinicadental.repository.SecretariaRepository;
import com.teo1.clinicadental.repository.UsuarioRepository;
import com.teo1.clinicadental.security.JwtService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final DoctorRepository doctorRepository;
    private final SecretariaRepository secretariaRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public RegistroResponse registrar(RegistroRequest request) {
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El email ya está registrado");
        }

        if (clienteRepository.existsByDpi(request.getDpi())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El DPI ya está registrado");
        }

        RolEntity rolCliente = rolRepository.findByNombreRol(Rol.CLIENTE)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        "El rol CLIENTE no está configurado en la base de datos"
                ));

        Usuario usuario = Usuario.builder()
                .nombre(request.getNombre())
                .apellido(request.getApellido())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .telefono(request.getTelefono())
                .rol(rolCliente)
                .estado(EstadoUsuario.ACTIVO)
                .build();

        Usuario usuarioGuardado = usuarioRepository.save(usuario);

        Cliente cliente = Cliente.builder()
                .usuario(usuarioGuardado)
                .dpi(request.getDpi())
                .direccion(request.getDireccion())
                .fechaNacimiento(request.getFechaNacimiento())
                .enListaNegra(false)
                .build();

        clienteRepository.save(cliente);

        return new RegistroResponse("Usuario registrado exitosamente", usuarioGuardado.getId());
    }

    public LoginResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Email o contraseña incorrectos"
                ));

        if (!passwordEncoder.matches(request.getPassword(), usuario.getPasswordHash())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Email o contraseña incorrectos"
            );
        }

        if (usuario.getEstado() == EstadoUsuario.INACTIVO) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La cuenta está inactiva");
        }

        String token = jwtService.generateToken(usuario.getId(), usuario.getRol().getNombreRol());

        return new LoginResponse(token);
    }

    @Transactional(readOnly = true)
    public UsuarioActualResponse usuarioActual(UUID usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));

        Rol rol = usuario.getRol().getNombreRol();
        UUID idPerfil = switch (rol) {
            case CLIENTE -> clienteRepository.findByUsuarioId(usuarioId).map(Cliente::getId).orElse(null);
            case DOCTOR -> doctorRepository.findByUsuarioId(usuarioId).map(Doctor::getId).orElse(null);
            case SECRETARIA -> secretariaRepository.findByUsuarioId(usuarioId).map(Secretaria::getId).orElse(null);
            case ADMIN -> null;
        };

        return new UsuarioActualResponse(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getApellido(),
                usuario.getEmail(),
                usuario.getTelefono(),
                rol.name(),
                idPerfil
        );
    }
}
