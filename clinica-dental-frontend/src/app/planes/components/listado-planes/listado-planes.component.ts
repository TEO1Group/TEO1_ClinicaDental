import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { PlanTratamientoResponse } from '../../models/plan-tratamiento.model';
import { PlanTratamientoService } from '../../service/plan-tratamiento.service';

@Component({
  selector: 'app-listado-planes',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './listado-planes.component.html',
  styleUrl: './listado-planes.component.scss'
})
export class ListadoPlanesComponent implements OnInit {
  private readonly planService = inject(PlanTratamientoService);
  private readonly authService = inject(AuthService);

  readonly rol = this.authService.rol;
  readonly planes = signal<PlanTratamientoResponse[]>([]);
  readonly cargando = signal(false);
  readonly error = signal('');

  ngOnInit(): void {
    this.cargarPlanes();
  }

  cargarPlanes(): void {
    this.cargando.set(true);
    this.error.set('');

    this.planService.listarPlanes().subscribe({
      next: (planes) => {
        this.planes.set(planes);
        this.cargando.set(false);
      },
      error: (error) => {
        this.cargando.set(false);
        this.error.set(error.error?.mensaje || 'Error al cargar los planes de tratamiento.');
      }
    });
  }

  puedeCrear(): boolean {
    return ['ADMIN', 'SECRETARIA', 'DOCTOR'].includes(this.rol() || '');
  }

  claseEstado(estado: string): string {
    return {
      ACTIVO: 'bg-primary',
      COMPLETADO: 'bg-success',
      CANCELADO: 'bg-danger'
    }[estado] || 'text-bg-light';
  }
}