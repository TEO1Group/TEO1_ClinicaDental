import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { NotificacionService } from '../../../core/notificacion/service/notificacion.service';
import { DoctorResponse } from '../../../doctores/models/doctor.model';
import { HorarioResponse } from '../../../doctores/models/horario.mode';
import { DoctorService } from '../../../doctores/service/doctor.service';
import { ClienteResponse } from '../../../pacientes/models/paciente.model';
import { PacienteService } from '../../../pacientes/service/paciente.service';
import { CitaRequest } from '../../models/cita.model';
import { CitaService } from '../../service/cita.service';

@Component({
  selector: 'app-formulario-cita',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './formulario-cita.component.html',
  styleUrl: './formulario-cita.component.scss'
})
export class FormularioCitaComponent implements OnInit {
  private readonly formBuilder = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly citaService = inject(CitaService);
  private readonly doctorService = inject(DoctorService);
  private readonly pacienteService = inject(PacienteService);
  private readonly notificacionService = inject(NotificacionService);
  private readonly router = inject(Router);

  readonly rol = this.authService.rol;
  readonly doctores = signal<DoctorResponse[]>([]);
  readonly pacientes = signal<ClienteResponse[]>([]);
  readonly horarios = signal<HorarioResponse[]>([]);
  readonly cargandoDoctores = signal(false);
  readonly cargandoPacientes = signal(false);
  readonly cargandoHorarios = signal(false);
  readonly isSubmitting = signal(false);
  readonly error = signal('');
  readonly fechaMinima = new Date().toISOString().slice(0, 10);

  readonly formulario = this.formBuilder.nonNullable.group({
    idDoctor: ['', [Validators.required]],
    idCliente: [''],
    fecha: ['', [Validators.required]],
    hora: ['', [Validators.required]],
    notas: ['', [Validators.maxLength(500)]]
  });

  ngOnInit(): void {
    this.cargarDoctores();
    if (this.puedeElegirPaciente()) {
      this.cargarPacientes();
    }

    this.formulario.controls.idDoctor.valueChanges.subscribe(idDoctor => {
      this.horarios.set([]);
      if (idDoctor) {
        this.cargarHorarios(idDoctor);
      }
    });
  }

  puedeElegirPaciente(): boolean {
    return this.rol() === 'ADMIN' || this.rol() === 'SECRETARIA';
  }

  cargarDoctores(): void {
    this.cargandoDoctores.set(true);
    this.doctorService.listarDoctores().subscribe({
      next: (doctores) => {
        this.doctores.set(doctores);
        this.cargandoDoctores.set(false);
      },
      error: (error) => {
        this.cargandoDoctores.set(false);
        this.error.set(error.error?.mensaje || 'Error al cargar los doctores.');
      }
    });
  }

  cargarPacientes(): void {
    this.cargandoPacientes.set(true);
    this.pacienteService.listarPacientes().subscribe({
      next: (pacientes) => {
        this.pacientes.set(pacientes);
        this.cargandoPacientes.set(false);
      },
      error: (error) => {
        this.cargandoPacientes.set(false);
        this.error.set(error.error?.mensaje || 'Error al cargar los pacientes.');
      }
    });
  }

  cargarHorarios(idDoctor: string): void {
    this.cargandoHorarios.set(true);
    this.doctorService.listarHorarios(idDoctor).subscribe({
      next: (horarios) => {
        this.horarios.set(horarios);
        this.cargandoHorarios.set(false);
      },
      error: (error) => {
        this.cargandoHorarios.set(false);
        this.error.set(error.error?.mensaje || 'Error al cargar los horarios del doctor.');
      }
    });
  }

  agendar(): void {
    if (this.formulario.invalid) {
      this.formulario.markAllAsTouched();
      return;
    }

    this.isSubmitting.set(true);
    this.error.set('');
    const valores = this.formulario.getRawValue();
    const request: CitaRequest = {
      idDoctor: valores.idDoctor,
      fecha: valores.fecha,
      hora: valores.hora,
      notas: valores.notas || undefined
    };

    if (this.puedeElegirPaciente()) {
      request.idCliente = valores.idCliente || undefined;
    }

    this.citaService.agendarCita(request).subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.notificacionService.exito('Cita agendada correctamente.');
        this.router.navigate(['/citas']);
      },
      error: (error) => {
        this.isSubmitting.set(false);
        this.notificacionService.error(error.error?.mensaje || 'Error al agendar la cita.');
      }
    });
  }
}