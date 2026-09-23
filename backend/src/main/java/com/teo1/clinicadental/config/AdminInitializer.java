package com.teo1.clinicadental.config;

import com.teo1.clinicadental.model.EstadoUsuario;
import com.teo1.clinicadental.model.Rol;
import com.teo1.clinicadental.model.RolEntity;
import com.teo1.clinicadental.model.Usuario;
import com.teo1.clinicadental.repository.RolRepository;
import com.teo1.clinicadental.repository.UsuarioRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Crea el primer administrador con ADMIN_EMAIL y ADMIN_PASSWORD si la base de datos
 * todavia no tiene ninguno. Si ya existe un ADMIN no hace nada.
 */
@Slf4j
@Component
public class AdminInitializer implements ApplicationRunner {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String password;

    public AdminInitializer(
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.email:}") String email,
            @Value("${app.admin.password:}") String password
    ) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (email.isBlank() || password.isBlank()) {
            return;
        }

        if (usuarioRepository.existsByRolNombreRol(Rol.ADMIN)) {
            return;
        }

        if (usuarioRepository.existsByEmail(email)) {
            log.warn("No se creo el administrador inicial: el email {} ya pertenece a otro usuario", email);
            return;
        }

        RolEntity rolAdmin = rolRepository.findByNombreRol(Rol.ADMIN)
                .orElseThrow(() -> new IllegalStateException("El rol ADMIN no esta configurado en la base de datos"));

        usuarioRepository.save(Usuario.builder()
                .nombre("Administrador")
                .apellido("Sistema")
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .rol(rolAdmin)
                .estado(EstadoUsuario.ACTIVO)
                .build());

        log.info("Administrador inicial creado con el email {}", email);
    }
}
