package com.teo1.clinicadental.service;

import com.teo1.clinicadental.dto.CitaResponse;
import com.teo1.clinicadental.model.Cita;
import org.springframework.stereotype.Component;

@Component
public class CitaMapper {

    public CitaResponse toResponse(Cita cita) {
        return new CitaResponse(
                cita.getId(),
                cita.getCliente().getId(),
                cita.getDoctor().getId(),
                cita.getFecha(),
                cita.getHora(),
                cita.getEstado(),
                cita.getNotas(),
                cita.getCliente().getUsuario().getNombre(),
                cita.getCliente().getUsuario().getApellido(),
                cita.getDoctor().getUsuario().getNombre(),
                cita.getDoctor().getUsuario().getApellido()
        );
    }
}
