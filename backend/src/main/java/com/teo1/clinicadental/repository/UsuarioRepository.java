package com.teo1.clinicadental.repository;

import java.util.Optional;
import java.util.UUID;
import com.teo1.clinicadental.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    boolean existsByEmail(String email);

    Optional<Usuario> findByEmail(String email);
}
