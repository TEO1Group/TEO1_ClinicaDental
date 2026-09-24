import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ClienteUpdateRequest } from '../../models/paciente.model';
import { PacienteService } from '../../service/paciente.service';

@Component({
  selector: 'app-formulario-paciente',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './formulario-paciente.component.html',
  styleUrl: './formulario-paciente.component.scss'
})
export class FormularioPacienteComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly pacienteService = inject(PacienteService);
  private readonly formBuilder = inject(FormBuilder);

  readonly editarForm = this.formBuilder.nonNullable.group({
    telefono: [''],
    direccion: [''],
    fechaNacimiento: ['']
  });

  private readonly _nombrePaciente = signal('');
  private readonly _cargando = signal(false);
  private readonly _guardando = signal(false);
  private readonly _error = signal('');
  private readonly _mensaje = signal('');

  readonly nombrePaciente = this._nombrePaciente.asReadonly();
  readonly cargando = this._cargando.asReadonly();
  readonly guardando = this._guardando.asReadonly();
  readonly error = this._error.asReadonly();
  readonly mensaje = this._mensaje.asReadonly();

  pacienteId = '';

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
        this._nombrePaciente.set(`${paciente.nombre} ${paciente.apellido}`);
        this.editarForm.patchValue({
          telefono: paciente.telefono || '',
          direccion: paciente.direccion || '',
          fechaNacimiento: paciente.fechaNacimiento || ''
        });
        this._cargando.set(false);
      },
      error: (error) => {
        this._cargando.set(false);
        this._error.set(error.error?.mensaje || 'Error al cargar el paciente.');
      }
    });
  }

  guardarCambios(): void {
    this._error.set('');
    this._mensaje.set('');

    if (this.editarForm.invalid) {
      this.editarForm.markAllAsTouched();
      return;
    }

    const valores = this.editarForm.getRawValue();
    const request: ClienteUpdateRequest = {
      telefono: valores.telefono.trim() || null,
      direccion: valores.direccion.trim() || null,
      fechaNacimiento: valores.fechaNacimiento || null
    };

    this._guardando.set(true);

    this.pacienteService.actualizarPaciente(this.pacienteId, request).subscribe({
      next: () => {
        this._guardando.set(false);
        this._mensaje.set('Paciente actualizado correctamente.');
        setTimeout(() => this.router.navigate(['/pacientes', this.pacienteId]), 800);
      },
      error: (error) => {
        this._guardando.set(false);
        this._error.set(error.error?.mensaje || 'Error al actualizar el paciente.');
      }
    });
  }
}
