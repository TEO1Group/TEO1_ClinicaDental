package com.teo1.clinicadental.repository;

import java.util.UUID;
import com.teo1.clinicadental.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepository extends JpaRepository<Cliente, UUID> {

    boolean existsByDpi(String dpi);
}
