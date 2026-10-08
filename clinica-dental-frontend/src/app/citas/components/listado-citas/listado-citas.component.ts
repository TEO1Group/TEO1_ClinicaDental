import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { toSignal } from '@angular/core/rxjs-interop';
import { NotificacionService } from '../../../core/notificacion/service/notificacion.service';
import { AuthService } from '../../../core/services/auth.service';
import { CitaResponse, EstadoCita, EstadoCitaRequest } from '../../models/cita.model';
import { CitaService } from '../../service/cita.service';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-listado-citas',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './listado-citas.component.html',
  styleUrl: './listado-citas.component.scss'
})
export class ListadoCitasComponent implements OnInit {
  private readonly citaService = inject(CitaService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly notificacionService = inject(NotificacionService);

  readonly rol = this.authService.rol;
  readonly filtroForm = this.formBuilder.nonNullable.group({
    estado: [''],
    fecha: ['']
  });

  private readonly estadoFiltro = toSignal(this.filtroForm.controls.estado.valueChanges, { initialValue: '' });
  private readonly fechaFiltro = toSignal(this.filtroForm.controls.fecha.valueChanges, { initialValue: '' });

  private readonly _citas = signal<CitaResponse[]>([]);
  private readonly _cargando = signal(false);
  private readonly _error = signal('');

  readonly citas = this._citas.asReadonly();
  readonly cargando = this._cargando.asReadonly();
  readonly error = this._error.asReadonly();

  readonly citasFiltradas = computed(() => {
    const estado = this.estadoFiltro();
    const fecha = this.fechaFiltro();

    return this._citas().filter(cita =>
      (!estado || cita.estado === estado) && (!fecha || cita.fecha === fecha)
    );
  });

  ngOnInit(): void {
    this.cargarCitas();
  }

  cargarCitas(): void {
    this._cargando.set(true);
    this._error.set('');

    this.citaService.listarCitas().subscribe({
      next: (citas) => {
        this._citas.set(citas);
        this._cargando.set(false);
      },
      error: (error) => {
        this._cargando.set(false);
        this._error.set(error.error?.mensaje || 'Error al cargar las citas.');
      }
    });
  }

  puedeCancelar(cita: CitaResponse): boolean {
    return cita.estado === 'AGENDADA' && ['CLIENTE', 'SECRETARIA', 'ADMIN'].includes(this.rol() || '');
  }

  puedeMarcarAtendida(cita: CitaResponse): boolean {
    return cita.estado === 'AGENDADA' && ['DOCTOR', 'ADMIN'].includes(this.rol() || '');
  }

  puedeMarcarNoAsistio(cita: CitaResponse): boolean {
    return cita.estado === 'AGENDADA' && ['DOCTOR', 'ADMIN'].includes(this.rol() || '');
  }

  cambiarEstado(cita: CitaResponse, estado: EstadoCita): void {
    const acciones: Record<EstadoCita, string> = {
      AGENDADA: 'reagendar',
      ATENDIDA: 'marcar como atendida',
      CANCELADA: 'cancelar',
      NO_ASISTIO: 'marcar como no asistida'
    };

    if (!confirm(`¿Estás seguro de ${acciones[estado]} esta cita?`)) {
      return;
    }

    const request: EstadoCitaRequest = { estado };
    this.citaService.cambiarEstado(cita.idCita, request).subscribe({
      next: () => {
        this.notificacionService.exito('Estado de la cita actualizado correctamente.');
        this.cargarCitas();
      },
      error: (error) => {
        this.notificacionService.error(error.error?.mensaje || 'Error al cambiar el estado de la cita.');
      }
    });
  }

  nombreDoctor(cita: CitaResponse): string {
    return [cita.nombreDoctor, cita.apellidoDoctor].filter(Boolean).join(' ') || cita.idDoctor;
  }

  nombrePaciente(cita: CitaResponse): string {
    return [
      cita.nombrePaciente ?? cita.nombreCliente,
      cita.apellidoPaciente ?? cita.apellidoCliente
    ].filter(Boolean).join(' ') || cita.idCliente;
  }

  etiquetaEstado(estado: EstadoCita): string {
    return {
      AGENDADA: 'Agendada',
      ATENDIDA: 'Atendida',
      CANCELADA: 'Cancelada',
      NO_ASISTIO: 'No asistió'
    }[estado];
  }

  claseEstado(estado: EstadoCita): string {
    return {
      AGENDADA: 'bg-primary',
      ATENDIDA: 'bg-success',
      CANCELADA: 'bg-danger',
      NO_ASISTIO: 'bg-warning text-dark'
    }[estado];
  }

  puedeCrear(): boolean {
    const rol = this.rol();
    return rol === 'CLIENTE' || rol === 'SECRETARIA' || rol === 'ADMIN';
  }
}
