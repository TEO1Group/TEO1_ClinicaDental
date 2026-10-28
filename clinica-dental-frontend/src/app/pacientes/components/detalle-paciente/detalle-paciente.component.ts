import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { NotificacionService } from '../../../core/notificacion/service/notificacion.service';
import { ClienteResponse, InasistenciasPacienteResponse } from '../../models/paciente.model';
import { PacienteService } from '../../service/paciente.service';

@Component({
  selector: 'app-detalle-paciente',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './detalle-paciente.component.html',
  styleUrl: './detalle-paciente.component.scss'
})
export class DetallePacienteComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly pacienteService = inject(PacienteService);
  private readonly authService = inject(AuthService);
  private readonly notificacionService = inject(NotificacionService);

  readonly rol = this.authService.rol;

  private readonly _paciente = signal<ClienteResponse | null>(null);
  private readonly _cargando = signal(false);
  private readonly _error = signal('');

  readonly paciente = this._paciente.asReadonly();
  readonly cargando = this._cargando.asReadonly();
  readonly error = this._error.asReadonly();

  private readonly _inasistencias = signal<InasistenciasPacienteResponse | null>(null);
  private readonly _cargandoInasistencias = signal(false);

  readonly inasistencias = this._inasistencias.asReadonly();
  readonly cargandoInasistencias = this._cargandoInasistencias.asReadonly();

  readonly mostrarModalLevantamiento = signal(false);
  readonly motivoLevantamiento = signal('');
  readonly errorLevantamiento = signal('');
  readonly enviandoLevantamiento = signal(false);

  private pacienteId = '';

  ngOnInit(): void {
    this.pacienteId = this.route.snapshot.paramMap.get('id') || '';

    if (this.pacienteId) {
      this.cargarPaciente();
      if (this.puedeVerInasistencias()) {
        this.cargarInasistencias();
      }
    } else {
      this._error.set('No se recibió un identificador de paciente válido.');
    }
  }

  cargarPaciente(): void {
    this._cargando.set(true);
    this._error.set('');

    this.pacienteService.obtenerPaciente(this.pacienteId).subscribe({
      next: (paciente) => {
        this._paciente.set(paciente);
        this._cargando.set(false);
      },
      error: (error) => {
        this._cargando.set(false);
        if (error.status === 403) return;
        this._error.set(error.error?.mensaje || 'Error al cargar el paciente.');
      }
    });
  }

  cargarInasistencias(): void {
    this._cargandoInasistencias.set(true);

    this.pacienteService.obtenerInasistencias(this.pacienteId).subscribe({
      next: (response) => {
        this._inasistencias.set(response);
        this._cargandoInasistencias.set(false);
      },
      error: () => {
        this._cargandoInasistencias.set(false);
      }
    });
  }

  puedeEditar(): boolean {
    return this.rol() === 'ADMIN' || this.rol() === 'SECRETARIA';
  }

  puedeGestionarListaNegra(): boolean {
    return this.rol() === 'SECRETARIA';
  }

  puedeVerHistorial(): boolean {
    const rol = this.rol();
    return rol === 'ADMIN' || rol === 'DOCTOR' || rol === 'SECRETARIA';
  }

  puedeVerInasistencias(): boolean {
    const rol = this.rol();
    return rol === 'ADMIN' || rol === 'DOCTOR' || rol === 'SECRETARIA';
  }

  puedeLevantarRestriccion(): boolean {
    const rol = this.rol();
    return rol === 'ADMIN' || rol === 'SECRETARIA';
  }

  desactivarPaciente(): void {
    const paciente = this._paciente();
    if (!paciente || !confirm(`¿Estás seguro de desactivar a ${paciente.nombre} ${paciente.apellido}?`)) {
      return;
    }

    this.pacienteService.desactivarPaciente(paciente.idCliente).subscribe({
      next: () => {
        this.notificacionService.exito('Paciente desactivado correctamente.');
        window.history.back();
      },
      error: (error) => {
        if (error.status === 403) return;
        this.notificacionService.error(error.error?.mensaje || 'Error al desactivar el paciente.');
      }
    });
  }

  // Modal de levantamiento
  abrirModalLevantamiento(): void {
    this.motivoLevantamiento.set('');
    this.errorLevantamiento.set('');
    this.mostrarModalLevantamiento.set(true);
  }

  cerrarModalLevantamiento(): void {
    this.mostrarModalLevantamiento.set(false);
    this.motivoLevantamiento.set('');
    this.errorLevantamiento.set('');
  }

  confirmarLevantamiento(): void {
    const motivo = this.motivoLevantamiento().trim();

    if (!motivo) {
      this.errorLevantamiento.set('El motivo es obligatorio.');
      return;
    }

    if (motivo.length > 200) {
      this.errorLevantamiento.set('El motivo no puede superar 200 caracteres.');
      return;
    }

    this.enviandoLevantamiento.set(true);
    this.errorLevantamiento.set('');

    this.pacienteService.levantarRestriccion(this.pacienteId, { motivo }).subscribe({
      next: (response) => {
        this.enviandoLevantamiento.set(false);
        this._inasistencias.set(response);
        this.notificacionService.exito('Restricción levantada correctamente.');
        this.cerrarModalLevantamiento();
      },
      error: (error) => {
        this.enviandoLevantamiento.set(false);
        if (error.status === 403) return;
        this.errorLevantamiento.set(error.error?.mensaje || 'Error al levantar la restricción.');
      }
    });
  }

  formatearFecha(fechaHora: string): string {
    const [fecha, hora] = fechaHora.split('T');
    const [year, month, day] = fecha.split('-');
    return `${day}/${month}/${year} ${hora.slice(0, 5)}`;
  }
}