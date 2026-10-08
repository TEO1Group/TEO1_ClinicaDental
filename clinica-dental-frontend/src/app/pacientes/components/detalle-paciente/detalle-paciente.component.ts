import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { NotificacionService } from '../../../core/notificacion/service/notificacion.service';
import { ClienteResponse } from '../../models/paciente.model';
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

  private pacienteId = '';

  ngOnInit(): void {
    this.pacienteId = this.route.snapshot.paramMap.get('id') || '';

    if (this.pacienteId) {
      this.cargarPaciente();
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
        this._error.set(error.error?.mensaje || 'Error al cargar el paciente.');
      }
    });
  }

  puedeEditar(): boolean {
    return this.rol() === 'ADMIN' || this.rol() === 'SECRETARIA';
  }

  puedeGestionarListaNegra(): boolean {
    return this.rol() === 'SECRETARIA';
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
        this.notificacionService.error(error.error?.mensaje || 'Error al desactivar el paciente.');
      }
    });
  }

  puedeVerHistorial(): boolean {
    const rol = this.rol();
    return rol === 'ADMIN' || rol === 'DOCTOR' || rol === 'SECRETARIA';
  }
}
