package com.teo1.clinicadental.controller;

import com.teo1.clinicadental.dto.DoctorResponse;
import com.teo1.clinicadental.dto.DoctorUpdateRequest;
import com.teo1.clinicadental.dto.HorarioRequest;
import com.teo1.clinicadental.dto.HorarioResponse;
import com.teo1.clinicadental.service.DoctorService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/doctores")
@RequiredArgsConstructor
public class DoctorController {

    private final DoctorService doctorService;

    @GetMapping
    public ResponseEntity<List<DoctorResponse>> listarDoctores() {
        return ResponseEntity.ok(doctorService.listarDoctores());
    }

    @GetMapping("/{id}")
    public ResponseEntity<DoctorResponse> obtenerDoctor(@PathVariable UUID id) {
        return ResponseEntity.ok(doctorService.obtenerDoctor(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<DoctorResponse> actualizarDoctor(
            @PathVariable UUID id,
            @Valid @RequestBody DoctorUpdateRequest request
    ) {
        return ResponseEntity.ok(doctorService.actualizarDoctor(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivarDoctor(@PathVariable UUID id) {
        doctorService.desactivarDoctor(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/horarios")
    public ResponseEntity<List<HorarioResponse>> listarHorarios(@PathVariable UUID id) {
        return ResponseEntity.ok(doctorService.listarHorarios(id));
    }

    @PostMapping("/{id}/horarios")
    public ResponseEntity<HorarioResponse> agregarHorario(
            @PathVariable UUID id,
            @Valid @RequestBody HorarioRequest request,
            Authentication authentication
    ) {
        HorarioResponse response = doctorService.agregarHorario(id, request, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}/horarios/{idHorario}")
    public ResponseEntity<HorarioResponse> actualizarHorario(
            @PathVariable UUID id,
            @PathVariable UUID idHorario,
            @Valid @RequestBody HorarioRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(doctorService.actualizarHorario(id, idHorario, request, authentication));
    }

    @DeleteMapping("/{id}/horarios/{idHorario}")
    public ResponseEntity<Void> eliminarHorario(
            @PathVariable UUID id,
            @PathVariable UUID idHorario,
            Authentication authentication
    ) {
        doctorService.eliminarHorario(id, idHorario, authentication);
        return ResponseEntity.noContent().build();
    }
}
