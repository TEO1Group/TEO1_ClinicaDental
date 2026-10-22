import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { NotificacionService } from '../../../core/notificacion/service/notificacion.service';
import { AuthService } from '../../../core/services/auth.service';
import { CitaResponse } from '../../../citas/models/cita.model';
import { PlanTratamientoResponse, SesionPlanRequest } from '../../models/plan-tratamiento.model';
import { PlanTratamientoService } from '../../service/plan-tratamiento.service';

@Component({
  selector: 'app-detalle-plan',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './detalle-plan.component.html',
  styleUrl: './detalle-plan.component.scss'
})
export class DetallePlanComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly planService = inject(PlanTratamientoService);
  private readonly authService = inject(AuthService);
  private readonly notificacionService = inject(NotificacionService);
  private readonly formBuilder = inject(FormBuilder);

  readonly rol = this.authService.rol;
  readonly plan = signal<PlanTratamientoResponse | null>(null);
  readonly cargando = signal(false);
  readonly guardando = signal(false);
  readonly error = signal('');
  readonly fechaMinima = this.formatearFecha(new Date());
  readonly sesionForm = this.formBuilder.nonNullable.group({
    fecha: ['', [Validators.required]],
    hora: ['', [Validators.required]],
    notas: ['', [Validators.maxLength(500)]]
  });

  private planId = '';

  ngOnInit(): void {
    this.planId = this.route.snapshot.paramMap.get('id') || '';
    if (this.planId) {
      this.cargarPlan();
    } else {
      this.error.set('No se recibió un identificador de plan válido.');
    }
  }

  cargarPlan(): void {
    this.cargando.set(true);
    this.error.set('');
    this.planService.obtenerPlan(this.planId).subscribe({
      next: (plan) => {
        this.plan.set(plan);
        this.cargando.set(false);
      },
      error: (error) => {
        this.cargando.set(false);
        this.error.set(error.error?.mensaje || 'Error al cargar el plan.');
      }
    });
  }

  puedeAgendarSesion(): boolean {
    return this.rol() === 'ADMIN' || this.rol() === 'SECRETARIA';
  }

  agendarSesion(): void {
    this.sesionForm.markAllAsTouched();
    if (this.sesionForm.invalid) {
      return;
    }

    const valores = this.sesionForm.getRawValue();
    const request: SesionPlanRequest = {
      fecha: valores.fecha,
      hora: valores.hora,
      notas: valores.notas || undefined
    };

    this.guardando.set(true);
    this.planService.agendarSesion(this.planId, request).subscribe({
      next: () => {
        this.guardando.set(false);
        this.sesionForm.reset();
        this.notificacionService.exito('Sesión agendada correctamente.');
        this.cargarPlan();
      },
      error: (error) => {
        this.guardando.set(false);
        this.notificacionService.error(error.error?.mensaje || 'No se pudo agendar la sesión.');
      }
    });
  }

  citas(): CitaResponse[] {
    return this.plan()?.citas || [];
  }

  claseEstado(estado: string): string {
    return {
      AGENDADA: 'bg-primary',
      ATENDIDA: 'bg-success',
      CANCELADA: 'bg-danger',
      NO_ASISTIO: 'bg-warning text-dark'
    }[estado] || 'text-bg-light';
  }

  private formatearFecha(fecha: Date): string {
    const mes = String(fecha.getMonth() + 1).padStart(2, '0');
    const dia = String(fecha.getDate()).padStart(2, '0');
    return `${fecha.getFullYear()}-${mes}-${dia}`;
  }
}