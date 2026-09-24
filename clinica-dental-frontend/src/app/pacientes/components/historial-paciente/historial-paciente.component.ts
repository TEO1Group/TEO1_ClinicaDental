import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { HistorialClinicoResponse } from '../../models/historial-clinico.model';
import { PacienteService } from '../../service/paciente.service';

@Component({
  selector: 'app-historial-paciente',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './historial-paciente.component.html',
  styleUrl: './historial-paciente.component.scss'
})
export class HistorialPacienteComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly pacienteService = inject(PacienteService);
  private readonly authService = inject(AuthService);
  private readonly formBuilder = inject(FormBuilder);

  readonly rol = this.authService.rol;
  readonly historialForm = this.formBuilder.nonNullable.group({
    descripcion: ['', [Validators.required]]
  });

  pacienteId = '';

  private readonly _nombrePaciente = signal('');
  private readonly _historial = signal<HistorialClinicoResponse[]>([]);
  private readonly _cargando = signal(false);
  private readonly _guardando = signal(false);
  private readonly _error = signal('');
  private readonly _errorFormulario = signal('');
  private readonly _mensaje = signal('');

  readonly nombrePaciente = this._nombrePaciente.asReadonly();
  readonly historial = this._historial.asReadonly();
  readonly cargando = this._cargando.asReadonly();
  readonly guardando = this._guardando.asReadonly();
  readonly error = this._error.asReadonly();
  readonly errorFormulario = this._errorFormulario.asReadonly();
  readonly mensaje = this._mensaje.asReadonly();

  ngOnInit(): void {
    this.pacienteId = this.route.snapshot.paramMap.get('id') || '';

    if (this.pacienteId) {
      this.cargarPaciente();
      this.cargarHistorial();
    } else {
      this._error.set('No se recibió un identificador de paciente válido.');
    }
  }

  cargarPaciente(): void {
    this.pacienteService.obtenerPaciente(this.pacienteId).subscribe({
      next: (paciente) => {
        this._nombrePaciente.set(`${paciente.nombre} ${paciente.apellido}`);
      },
      error: () => {
        // El historial puede seguir consultándose aunque falle el encabezado.
      }
    });
  }

  cargarHistorial(): void {
    this._cargando.set(true);
    this._error.set('');

    this.pacienteService.listarHistorial(this.pacienteId).subscribe({
      next: (registros) => {
        this._historial.set(registros);
        this._cargando.set(false);
      },
      error: (error) => {
        this._cargando.set(false);
        this._error.set(error.error?.mensaje || 'Error al cargar el historial clínico.');
      }
    });
  }

  puedeAgregarHistorial(): boolean {
    const rol = this.rol();
    return rol === 'ADMIN' || rol === 'DOCTOR';
  }

  agregarRegistro(): void {
    this._errorFormulario.set('');
    this._mensaje.set('');

    if (this.historialForm.invalid) {
      this.historialForm.markAllAsTouched();
      return;
    }

    this._guardando.set(true);

    this.pacienteService.agregarHistorial(this.pacienteId, this.historialForm.getRawValue()).subscribe({
      next: (registro) => {
        this._historial.update(historial => [registro, ...historial]);
        this.historialForm.reset();
        this._guardando.set(false);
        this._mensaje.set('Registro agregado correctamente.');
      },
      error: (error) => {
        this._guardando.set(false);
        this._errorFormulario.set(error.error?.mensaje || 'Error al agregar el registro.');
      }
    });
  }
}
