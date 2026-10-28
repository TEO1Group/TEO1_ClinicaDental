import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PlanTratamientoService } from '../../service/plan-tratamiento.service';
import { PlanTratamientoResponse, EstadoPlan } from '../../models/plan-tratamiento.model';

@Component({
  selector: 'app-mis-planes',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './mis-planes.component.html',
  styleUrl: './mis-planes.component.scss'
})
export class MisPlanesComponent implements OnInit {
  private readonly planService = inject(PlanTratamientoService);

  private readonly _planes = signal<PlanTratamientoResponse[]>([]);
  private readonly _cargando = signal(false);
  private readonly _error = signal('');

  readonly planes = this._planes.asReadonly();
  readonly cargando = this._cargando.asReadonly();
  readonly error = this._error.asReadonly();

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this._cargando.set(true);
    this._error.set('');

    this.planService.listarMisPlanes().subscribe({
      next: (planes) => {
        this._planes.set(planes);
        this._cargando.set(false);
      },
      error: (error) => {
        this._cargando.set(false);
        if (error.status === 403) return;
        this._error.set(error.error?.mensaje || 'Error al cargar tus planes.');
      }
    });
  }

  claseEstado(estado: EstadoPlan): string {
    return {
      ACTIVO: 'bg-primary',
      COMPLETADO: 'bg-success',
      CANCELADO: 'bg-secondary'
    }[estado];
  }

  etiquetaEstado(estado: EstadoPlan): string {
    return {
      ACTIVO: 'Activo',
      COMPLETADO: 'Completado',
      CANCELADO: 'Cancelado'
    }[estado];
  }

  nombreDoctor(plan: PlanTratamientoResponse): string {
    return `Dr. ${plan.nombreDoctor} ${plan.apellidoDoctor}`;
  }
}