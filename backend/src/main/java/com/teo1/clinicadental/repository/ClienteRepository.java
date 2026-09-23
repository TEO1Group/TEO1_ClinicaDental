package com.teo1.clinicadental.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.teo1.clinicadental.model.Cliente;
import com.teo1.clinicadental.model.EstadoUsuario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepository extends JpaRepository<Cliente, UUID> {

    boolean existsByDpi(String dpi);

    List<Cliente> findByUsuarioEstado(EstadoUsuario estado);

    Optional<Cliente> findByUsuarioId(UUID usuarioId);
}
