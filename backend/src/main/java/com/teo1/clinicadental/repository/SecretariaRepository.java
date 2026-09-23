package com.teo1.clinicadental.repository;

import com.teo1.clinicadental.model.Secretaria;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SecretariaRepository extends JpaRepository<Secretaria, UUID> {

    Optional<Secretaria> findByUsuarioId(UUID usuarioId);
}
