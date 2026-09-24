import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { PacienteService } from '../models/service/paciente.service';
import { AuthService } from '../../core/services/auth.service';
import { NotificacionService } from '../../core/notificacion/service/notificacion.service';
import { ClienteResponse } from '../models/cliente.model';
import { HistorialClinicoResponse } from '../models/historial.model';

@Component({
  selector: 'app-detalle-paciente',
  standalone: true,
  imports: [CommonModule, RouterLink, ReactiveFormsModule],
  templateUrl: './detalle-paciente.component.html',
  styleUrl: './detalle-paciente.component.scss'
})
export class DetallePacienteComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly pacienteService = inject(PacienteService);
  private readonly authService = inject(AuthService);
  private readonly notificacionService = inject(NotificacionService);
  private readonly formBuilder = inject(FormBuilder);

  readonly rol = this.authService.rol;

  private readonly _paciente = signal<ClienteResponse | null>(null);
  private readonly _historial = signal<HistorialClinicoResponse[]>([]);
  private readonly _cargando = signal<boolean>(false);
  private readonly _error = signal<string>('');

  readonly paciente = this._paciente.asReadonly();
  readonly historial = this._historial.asReadonly();
  readonly cargando = this._cargando.asReadonly();
  readonly error = this._error.asReadonly();

  // Modal editar paciente
  mostrarModalEditar = false;
  isSubmittingEditar = false;

  readonly editarForm = this.formBuilder.nonNullable.group({
    nombre: ['', [Validators.maxLength(80)]],
    apellido: ['', [Validators.maxLength(80)]],
    telefono: ['', [Validators.pattern(/^[0-9]{8}$/)]],
    direccion: ['', [Validators.maxLength(200)]],
    fechaNacimiento: ['']
  });

  // Modal lista negra
  mostrarModalListaNegra = false;
  isSubmittingListaNegra = false;

  readonly listaNegraForm = this.formBuilder.nonNullable.group({
    enListaNegra: [false],
    motivo: ['', [Validators.maxLength(200)]]
  });

  // Formulario historial
  mostrarFormHistorial = false;
  isSubmittingHistorial = false;

  readonly historialForm = this.formBuilder.nonNullable.group({
    descripcion: ['', [Validators.required]]
  });

  private pacienteId = '';

  ngOnInit(): void {
    this.pacienteId = this.route.snapshot.paramMap.get('id') || '';
    if (this.pacienteId) {
      this.cargarPaciente();
      this.cargarHistorial();
    }
  }

  cargarPaciente(): void {
    this._cargando.set(true);
    this._error.set('');

    this.pacienteService.obtenerPaciente(this.pacienteId).subscribe({
      next: (response) => {
        this._paciente.set(response);
        this._cargando.set(false);
      },
      error: (error) => {
        this._cargando.set(false);
        this._error.set(error.error?.mensaje || 'Error al cargar el paciente.');
      }
    });
  }

  cargarHistorial(): void {
    this.pacienteService.listarHistorial(this.pacienteId).subscribe({
      next: (response) => {
        this._historial.set(response);
      },
      error: () => {
        // Silencioso
      }
    });
  }

  puedeEditar(): boolean {
    const rol = this.rol();
    return rol === 'ADMIN' || rol === 'SECRETARIA';
  }

  puedeDesactivar(): boolean {
    const rol = this.rol();
    return rol === 'ADMIN' || rol === 'SECRETARIA';
  }

  puedeGestionarListaNegra(): boolean {
    return this.rol() === 'SECRETARIA';
  }

  puedeAgregarHistorial(): boolean {
    const rol = this.rol();
    return rol === 'ADMIN' || rol === 'DOCTOR';
  }

  // Modal editar
  abrirModalEditar(): void {
    const pac = this._paciente();
    if (!pac) return;

    this.editarForm.patchValue({
      nombre: pac.nombre,
      apellido: pac.apellido,
      telefono: pac.telefono || '',
      direccion: pac.direccion || '',
      fechaNacimiento: pac.fechaNacimiento || ''
    });

    this.mostrarModalEditar = true;
  }

  cerrarModalEditar(): void {
    this.mostrarModalEditar = false;
    this.editarForm.reset();
  }

  guardarEdicion(): void {
    if (this.editarForm.invalid) {
      this.editarForm.markAllAsTouched();
      return;
    }

    this.isSubmittingEditar = true;

    const valores = this.editarForm.getRawValue();
    const request = {
      nombre: valores.nombre || undefined,
      apellido: valores.apellido || undefined,
      telefono: valores.telefono || undefined,
      direccion: valores.direccion || undefined,
      fechaNacimiento: valores.fechaNacimiento || undefined
    };

    this.pacienteService.actualizarPaciente(this.pacienteId, request).subscribe({
      next: (response) => {
        this.isSubmittingEditar = false;
        this._paciente.set(response);
        this.notificacionService.exito('Paciente actualizado correctamente.');
        this.cerrarModalEditar();
      },
      error: (error) => {
        this.isSubmittingEditar = false;
        this.notificacionService.error(error.error?.mensaje || 'Error al actualizar el paciente.');
      }
    });
  }

  desactivarPaciente(): void {
    const pac = this._paciente();
    if (!pac) return;

    if (!confirm(`¿Estás seguro de desactivar a ${pac.nombre} ${pac.apellido}? Su historial se conservará.`)) {
      return;
    }

    this.pacienteService.desactivarPaciente(this.pacienteId).subscribe({
      next: () => {
        this.notificacionService.exito('Paciente desactivado correctamente.');
        window.history.back();
      },
      error: (error) => {
        this.notificacionService.error(error.error?.mensaje || 'Error al desactivar el paciente.');
      }
    });
  }

  // Modal lista negra
  abrirModalListaNegra(): void {
    const pac = this._paciente();
    if (!pac) return;

    this.listaNegraForm.patchValue({
      enListaNegra: pac.enListaNegra,
      motivo: pac.motivoListaNegra || ''
    });

    this.mostrarModalListaNegra = true;
  }

  cerrarModalListaNegra(): void {
    this.mostrarModalListaNegra = false;
    this.listaNegraForm.reset({ enListaNegra: false, motivo: '' });
  }

  guardarListaNegra(): void {
    const valores = this.listaNegraForm.getRawValue();

    if (valores.enListaNegra && !valores.motivo.trim()) {
      this.notificacionService.error('El motivo es obligatorio para agregar a la lista negra.');
      return;
    }

    this.isSubmittingListaNegra = true;

    const request = {
      enListaNegra: valores.enListaNegra,
      motivo: valores.enListaNegra ? valores.motivo : undefined
    };

    this.pacienteService.actualizarListaNegra(this.pacienteId, request).subscribe({
      next: (response) => {
        this.isSubmittingListaNegra = false;
        this._paciente.set(response);
        this.notificacionService.exito('Lista negra actualizada correctamente.');
        this.cerrarModalListaNegra();
      },
      error: (error) => {
        this.isSubmittingListaNegra = false;
        this.notificacionService.error(error.error?.mensaje || 'Error al actualizar la lista negra.');
      }
    });
  }

  // Historial
  toggleFormHistorial(): void {
    this.mostrarFormHistorial = !this.mostrarFormHistorial;
    this.historialForm.reset();
  }

  guardarHistorial(): void {
    if (this.historialForm.invalid) {
      this.historialForm.markAllAsTouched();
      return;
    }

    this.isSubmittingHistorial = true;

    this.pacienteService.agregarHistorial(this.pacienteId, this.historialForm.getRawValue()).subscribe({
      next: (response) => {
        this.isSubmittingHistorial = false;
        this._historial.update(h => [response, ...h]);
        this.notificacionService.exito('Entrada agregada al historial.');
        this.historialForm.reset();
        this.mostrarFormHistorial = false;
      },
      error: (error) => {
        this.isSubmittingHistorial = false;
        this.notificacionService.error(error.error?.mensaje || 'Error al agregar la entrada.');
      }
    });
  }
}