package com.teo1.clinicadental.repository;

import com.teo1.clinicadental.model.Tratamiento;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TratamientoRepository extends JpaRepository<Tratamiento, UUID> {

    List<Tratamiento> findAllByOrderByNombreAsc();

    List<Tratamiento> findByActivoTrueOrderByNombreAsc();

    boolean existsByNombre(String nombre);

    boolean existsByNombreAndIdNot(String nombre, UUID id);
}
