package com.teo1.clinicadental.config;

import com.teo1.clinicadental.model.Rol;
import com.teo1.clinicadental.model.RolEntity;
import com.teo1.clinicadental.model.Usuario;
import com.teo1.clinicadental.repository.RolRepository;
import com.teo1.clinicadental.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminInitializerTests {

    private UsuarioRepository usuarioRepository;
    private RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        usuarioRepository = mock(UsuarioRepository.class);
        rolRepository = mock(RolRepository.class);
        when(rolRepository.findByNombreRol(Rol.ADMIN))
                .thenReturn(Optional.of(RolEntity.builder().nombreRol(Rol.ADMIN).build()));
    }

    @Test
    void creaAdminCuandoNoExisteNinguno() {
        when(usuarioRepository.existsByRolNombreRol(Rol.ADMIN)).thenReturn(false);

        crear("admin@clinica.com", "Admin123!").run(null);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        Usuario admin = captor.getValue();
        assertEquals("admin@clinica.com", admin.getEmail());
        assertEquals(Rol.ADMIN, admin.getRol().getNombreRol());
        assertTrue(passwordEncoder.matches("Admin123!", admin.getPasswordHash()));
    }

    @Test
    void noCreaAdminSiYaExisteUno() {
        when(usuarioRepository.existsByRolNombreRol(Rol.ADMIN)).thenReturn(true);

        crear("admin@clinica.com", "Admin123!").run(null);

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void noCreaAdminSinVariablesDeEntorno() {
        crear("", "").run(null);

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void noCreaAdminSiElEmailYaEstaUsado() {
        when(usuarioRepository.existsByRolNombreRol(Rol.ADMIN)).thenReturn(false);
        when(usuarioRepository.existsByEmail("admin@clinica.com")).thenReturn(true);

        crear("admin@clinica.com", "Admin123!").run(null);

        verify(usuarioRepository, never()).save(any());
    }

    private AdminInitializer crear(String email, String password) {
        return new AdminInitializer(usuarioRepository, rolRepository, passwordEncoder, email, password);
    }
}
