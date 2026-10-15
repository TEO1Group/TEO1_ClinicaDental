package com.teo1.clinicadental.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.teo1.clinicadental.model.Cliente;
import com.teo1.clinicadental.model.EstadoUsuario;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepository extends JpaRepository<Cliente, UUID> {

    boolean existsByDpi(String dpi);

    @EntityGraph(attributePaths = {"usuario", "usuario.rol"})
    List<Cliente> findByUsuarioEstado(EstadoUsuario estado);

    @EntityGraph(attributePaths = {"usuario", "usuario.rol"})
    Optional<Cliente> findByUsuarioId(UUID usuarioId);

    @EntityGraph(attributePaths = {"usuario", "usuario.rol"})
    @Override
    Optional<Cliente> findById(UUID id);
}
