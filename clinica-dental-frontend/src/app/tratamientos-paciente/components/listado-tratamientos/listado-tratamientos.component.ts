import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NotificacionService } from '../../../core/notificacion/service/notificacion.service';
import { TratamientoService } from '../../service/tratamiento.service';
import {
  TratamientoRequest,
  TratamientoResponse
} from '../../models/tratamiento.model';

@Component({
  selector: 'app-listado-tratamientos',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './listado-tratamientos.component.html',
  styleUrl: './listado-tratamientos.component.scss'
})
export class ListadoTratamientosComponent implements OnInit {
  private readonly tratamientoService = inject(TratamientoService);
  private readonly notificacionService = inject(NotificacionService);
  private readonly formBuilder = inject(FormBuilder);

  private readonly _tratamientos = signal<TratamientoResponse[]>([]);
  private readonly _cargando = signal(false);
  private readonly _error = signal('');
  private readonly _incluirInactivos = signal(false);

  readonly tratamientos = this._tratamientos.asReadonly();
  readonly cargando = this._cargando.asReadonly();
  readonly error = this._error.asReadonly();
  readonly incluirInactivos = this._incluirInactivos.asReadonly();

  // Modal
  readonly modalAbierto = signal(false);
  readonly modoEdicion = signal(false);
  readonly tratamientoEditando = signal<TratamientoResponse | null>(null);
  readonly errorModal = signal('');
  readonly enviando = signal(false);

  readonly form = this.formBuilder.nonNullable.group({
    nombre: ['', [Validators.required, Validators.maxLength(100)]],
    descripcion: [''],
    duracionMinutos: [30, [Validators.required, Validators.min(1)]],
    costo: [0, [Validators.required, Validators.min(0)]],
    activo: [true]
  });

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this._cargando.set(true);
    this._error.set('');

    this.tratamientoService.listar(this._incluirInactivos()).subscribe({
      next: (response) => {
        this._tratamientos.set(response);
        this._cargando.set(false);
      },
      error: (error) => {
        this._cargando.set(false);
        if (error.status === 403) return;
        this._error.set(error.error?.mensaje || 'Error al cargar el catálogo.');
      }
    });
  }

  toggleIncluirInactivos(): void {
    this._incluirInactivos.update(v => !v);
    this.cargar();
  }

  abrirModalCrear(): void {
    this.modoEdicion.set(false);
    this.tratamientoEditando.set(null);
    this.form.reset({
      nombre: '',
      descripcion: '',
      duracionMinutos: 30,
      costo: 0,
      activo: true
    });
    this.errorModal.set('');
    this.modalAbierto.set(true);
  }

  abrirModalEditar(tratamiento: TratamientoResponse): void {
    this.modoEdicion.set(true);
    this.tratamientoEditando.set(tratamiento);
    this.form.reset({
      nombre: tratamiento.nombre,
      descripcion: tratamiento.descripcion || '',
      duracionMinutos: tratamiento.duracionMinutos,
      costo: tratamiento.costo,
      activo: tratamiento.activo
    });
    this.errorModal.set('');
    this.modalAbierto.set(true);
  }

  cerrarModal(): void {
    this.modalAbierto.set(false);
    this.tratamientoEditando.set(null);
    this.errorModal.set('');
  }

  guardar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const valores = this.form.getRawValue();
    const request: TratamientoRequest = {
      nombre: valores.nombre.trim(),
      descripcion: valores.descripcion.trim() || undefined,
      duracionMinutos: valores.duracionMinutos,
      costo: valores.costo,
      activo: valores.activo
    };

    this.enviando.set(true);
    this.errorModal.set('');

    const operacion = this.modoEdicion()
      ? this.tratamientoService.actualizar(this.tratamientoEditando()!.idTratamiento, request)
      : this.tratamientoService.crear(request);

    operacion.subscribe({
      next: () => {
        this.enviando.set(false);
        this.notificacionService.exito(
          this.modoEdicion() ? 'Tratamiento actualizado correctamente.' : 'Tratamiento creado correctamente.'
        );
        this.cerrarModal();
        this.cargar();
      },
      error: (error) => {
        this.enviando.set(false);
        if (error.status === 403) return;
        this.errorModal.set(error.error?.mensaje || 'Error al guardar el tratamiento.');
      }
    });
  }

  toggleActivo(tratamiento: TratamientoResponse): void {
    const accion = tratamiento.activo ? 'desactivar' : 'activar';
    if (!confirm(`¿Estás seguro de ${accion} el tratamiento "${tratamiento.nombre}"?`)) {
      return;
    }

    const request: TratamientoRequest = {
      nombre: tratamiento.nombre,
      descripcion: tratamiento.descripcion || undefined,
      duracionMinutos: tratamiento.duracionMinutos,
      costo: tratamiento.costo,
      activo: !tratamiento.activo
    };

    this.tratamientoService.actualizar(tratamiento.idTratamiento, request).subscribe({
      next: () => {
        this.notificacionService.exito(
          tratamiento.activo ? 'Tratamiento desactivado.' : 'Tratamiento activado.'
        );
        this.cargar();
      },
      error: (error) => {
        if (error.status === 403) return;
        this.notificacionService.error(error.error?.mensaje || 'Error al cambiar el estado.');
      }
    });
  }
}