import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { NotificacionService } from '../../../core/notificacion/service/notificacion.service';
import { AuthService } from '../../../core/services/auth.service';
import { CitaResponse } from '../../../citas/models/cita.model';
import {
  AbonoPlanResponse,
  AbonosPlanResponse,
  PlanTratamientoResponse,
  SesionPlanRequest
} from '../../models/plan-tratamiento.model';
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
  readonly abonosData = signal<AbonosPlanResponse | null>(null);
  readonly cargando = signal(false);
  readonly cargandoAbonos = signal(false);
  readonly guardando = signal(false);
  readonly error = signal('');
  readonly fechaMinima = this.formatearFecha(new Date());

  readonly sesionForm = this.formBuilder.nonNullable.group({
    fecha: ['', [Validators.required]],
    hora: ['', [Validators.required]],
    notas: ['', [Validators.maxLength(500)]]
  });

  // Modal de abono
  readonly modalAbonoAbierto = signal(false);
  readonly errorAbono = signal('');
  readonly enviandoAbono = signal(false);

  readonly abonoForm = this.formBuilder.nonNullable.group({
    monto: [0, [Validators.required, Validators.min(0.01)]]
  });

  private planId = '';

  ngOnInit(): void {
    this.planId = this.route.snapshot.paramMap.get('id') || '';
    if (this.planId) {
      this.cargarPlan();
      if (this.puedeVerAbonos()) {
        this.cargarAbonos();
      }
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
        if (error.status === 403) return;
        this.error.set(error.error?.mensaje || 'Error al cargar el plan.');
      }
    });
  }

  cargarAbonos(): void {
    this.cargandoAbonos.set(true);
    this.planService.listarAbonos(this.planId).subscribe({
      next: (response) => {
        this.abonosData.set(response);
        this.cargandoAbonos.set(false);
      },
      error: (error) => {
        this.cargandoAbonos.set(false);
        if (error.status === 403) return;
      }
    });
  }

  puedeAgendarSesion(): boolean {
    return this.rol() === 'ADMIN' || this.rol() === 'SECRETARIA';
  }

  puedeVerAbonos(): boolean {
    const rol = this.rol();
    return rol === 'ADMIN' || rol === 'SECRETARIA' || rol === 'DOCTOR';
  }

  puedeRegistrarAbono(): boolean {
    const rol = this.rol();
    return rol === 'ADMIN' || rol === 'SECRETARIA';
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
        if (error.status === 403) return;
        this.notificacionService.error(error.error?.mensaje || 'No se pudo agendar la sesión.');
      }
    });
  }

  // Modal de abono
  abrirModalAbono(): void {
    this.abonoForm.reset({ monto: 0 });
    this.errorAbono.set('');
    this.modalAbonoAbierto.set(true);
  }

  cerrarModalAbono(): void {
    this.modalAbonoAbierto.set(false);
    this.errorAbono.set('');
  }

  registrarAbono(): void {
    if (this.abonoForm.invalid) {
      this.abonoForm.markAllAsTouched();
      return;
    }

    const monto = this.abonoForm.controls.monto.value;
    if (monto <= 0) {
      this.errorAbono.set('El monto debe ser mayor a cero.');
      return;
    }

    this.enviandoAbono.set(true);
    this.errorAbono.set('');

    this.planService.registrarAbono(this.planId, { monto }).subscribe({
      next: () => {
        this.enviandoAbono.set(false);
        this.notificacionService.exito('Abono registrado correctamente.');
        this.cerrarModalAbono();
        this.cargarPlan();
        this.cargarAbonos();
      },
      error: (error) => {
        this.enviandoAbono.set(false);
        if (error.status === 403) return;
        this.errorAbono.set(error.error?.mensaje || 'Error al registrar el abono.');
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
      NO_ASISTIO: 'bg-warning text-dark',
      ACTIVO: 'bg-primary',
      COMPLETADO: 'bg-success',
      CANCELADO: 'bg-secondary'
    }[estado] || 'text-bg-light';
  }

  formatearFechaHora(fechaHora: string): string {
    const [fecha, hora] = fechaHora.split('T');
    const [year, month, day] = fecha.split('-');
    return `${day}/${month}/${year} ${hora ? hora.slice(0, 5) : ''}`;
  }

  private formatearFecha(fecha: Date): string {
    const mes = String(fecha.getMonth() + 1).padStart(2, '0');
    const dia = String(fecha.getDate()).padStart(2, '0');
    return `${fecha.getFullYear()}-${mes}-${dia}`;
  }
}