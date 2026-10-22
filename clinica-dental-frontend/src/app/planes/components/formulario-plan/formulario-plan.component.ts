import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { NotificacionService } from '../../../core/notificacion/service/notificacion.service';
import { DoctorResponse } from '../../../doctores/models/doctor.model';
import { DoctorService } from '../../../doctores/service/doctor.service';
import { ClienteResponse } from '../../../pacientes/models/paciente.model';
import { PacienteService } from '../../../pacientes/service/paciente.service';
import { TratamientoResponse } from '../../../pacientes/models/tratamiento.model';
import { TratamientoService } from '../../../pacientes/service/tratamiento.service';
import { PlanTratamientoRequest } from '../../models/plan-tratamiento.model';
import { PlanTratamientoService } from '../../service/plan-tratamiento.service';

@Component({
  selector: 'app-formulario-plan',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './formulario-plan.component.html',
  styleUrl: './formulario-plan.component.scss'
})
export class FormularioPlanComponent implements OnInit {
  private readonly formBuilder = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly planService = inject(PlanTratamientoService);
  private readonly doctorService = inject(DoctorService);
  private readonly pacienteService = inject(PacienteService);
  private readonly tratamientoService = inject(TratamientoService);
  private readonly notificacionService = inject(NotificacionService);
  private readonly router = inject(Router);

  readonly rol = this.authService.rol;
  readonly usuarioActual = this.authService.usuarioActual;
  readonly doctores = signal<DoctorResponse[]>([]);
  readonly pacientes = signal<ClienteResponse[]>([]);
  readonly tratamientos = signal<TratamientoResponse[]>([]);
  readonly cargando = signal(false);
  readonly guardando = signal(false);
  readonly error = signal('');

  readonly planForm = this.formBuilder.nonNullable.group({
    idCliente: ['', [Validators.required]],
    idDoctor: [''],
    idTratamiento: ['', [Validators.required]],
    totalSesiones: [1, [Validators.required, Validators.min(1), Validators.max(50)]],
    costoTotal: ['', [Validators.min(0), Validators.pattern(/^\d+(\.\d{1,2})?$/)]]
  });

  ngOnInit(): void {
    this.cargarDatos();
  }

  esDoctor(): boolean {
    return this.rol() === 'DOCTOR';
  }

  private cargarDatos(): void {
    this.cargando.set(true);
    this.pacienteService.listarPacientes().subscribe({
      next: (pacientes) => {
        this.pacientes.set(pacientes);
        this.cargarTratamientos();
        if (this.esDoctor()) {
          this.cargando.set(false);
        } else {
          this.cargarDoctores();
        }
      },
      error: (error) => this.finalizarCargaConError(error, 'Error al cargar los pacientes.')
    });
  }

  private cargarDoctores(): void {
    this.doctorService.listarDoctores().subscribe({
      next: (doctores) => {
        this.doctores.set(doctores);
        this.cargando.set(false);
      },
      error: (error) => this.finalizarCargaConError(error, 'Error al cargar los doctores.')
    });
  }

  private cargarTratamientos(): void {
    this.tratamientoService.listarCatalogo().subscribe({
      next: (tratamientos) => this.tratamientos.set(tratamientos),
      error: (error) => this.finalizarCargaConError(error, 'Error al cargar los tratamientos.')
    });
  }

  crearPlan(): void {
    this.planForm.markAllAsTouched();
    if (this.planForm.invalid) {
      return;
    }

    const valores = this.planForm.getRawValue();
    const request: PlanTratamientoRequest = {
      idCliente: valores.idCliente,
      idTratamiento: valores.idTratamiento,
      totalSesiones: valores.totalSesiones,
      costoTotal: valores.costoTotal ? Number(valores.costoTotal) : undefined
    };

    if (!this.esDoctor()) {
      request.idDoctor = valores.idDoctor || undefined;
    }

    this.guardando.set(true);
    this.planService.crearPlan(request).subscribe({
      next: (plan) => {
        this.guardando.set(false);
        this.notificacionService.exito('Plan de tratamiento creado correctamente.');
        this.router.navigate(['/planes', plan.idPlan]);
      },
      error: (error) => {
        this.guardando.set(false);
        this.notificacionService.error(error.error?.mensaje || 'Error al crear el plan.');
      }
    });
  }

  private finalizarCargaConError(error: { error?: { mensaje?: string } }, mensaje: string): void {
    this.cargando.set(false);
    this.error.set(error.error?.mensaje || mensaje);
  }
}