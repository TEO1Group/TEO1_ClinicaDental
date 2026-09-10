package com.teo1.clinicadental.service;

import com.teo1.clinicadental.dto.RegistroRequest;
import com.teo1.clinicadental.dto.RegistroResponse;
import com.teo1.clinicadental.model.Rol;
import com.teo1.clinicadental.model.Usuario;
import com.teo1.clinicadental.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public RegistroResponse registrar(RegistroRequest request) {
        if (usuarioRepository.existsByDpi(request.getDpi())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El DPI ya está registrado");
        }

        if (usuarioRepository.existsByCelular(request.getCelular())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "El número de celular ya está registrado"
            );
        }

        Usuario usuario = Usuario.builder()
                .dpi(request.getDpi())
                .nombre(request.getNombre())
                .celular(request.getCelular())
                .password(passwordEncoder.encode(request.getPassword()))
                .rol(Rol.CLIENTE)
                .build();

        Usuario usuarioGuardado = usuarioRepository.save(usuario);

        return new RegistroResponse("Usuario registrado exitosamente", usuarioGuardado.getId());
    }
}
