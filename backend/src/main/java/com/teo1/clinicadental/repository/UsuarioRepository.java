package com.teo1.clinicadental.repository;

import java.util.Optional;
import com.teo1.clinicadental.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    boolean existsByDpi(String dpi);

    boolean existsByCelular(String celular);

    Optional<Usuario> findByCelular(String celular);
}
