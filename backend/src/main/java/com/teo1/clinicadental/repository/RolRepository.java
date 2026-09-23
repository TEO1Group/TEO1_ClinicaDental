package com.teo1.clinicadental.repository;

import java.util.Optional;
import java.util.UUID;
import com.teo1.clinicadental.model.Rol;
import com.teo1.clinicadental.model.RolEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RolRepository extends JpaRepository<RolEntity, UUID> {

    Optional<RolEntity> findByNombreRol(Rol nombreRol);
}
