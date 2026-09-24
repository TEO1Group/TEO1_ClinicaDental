import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ListaNegraRequest } from '../../models/paciente.model';
import { PacienteService } from '../../service/paciente.service';

@Component({
  selector: 'app-formulario-lista-negra',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './formulario-lista-negra.component.html',
  styleUrl: './formulario-lista-negra.component.scss'
})
export class FormularioListaNegraComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly pacienteService = inject(PacienteService);
  private readonly formBuilder = inject(FormBuilder);

  readonly listaNegraForm = this.formBuilder.nonNullable.group({
    enListaNegra: [false],
    motivo: ['']
  });

  pacienteId = '';

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

  ngOnInit(): void {
    this.pacienteId = this.route.snapshot.paramMap.get('id') || '';

    this.listaNegraForm.controls.enListaNegra.valueChanges.subscribe((enListaNegra) => {
      const motivoControl = this.listaNegraForm.controls.motivo;
      if (enListaNegra) {
        motivoControl.addValidators(Validators.required);
      } else {
        motivoControl.removeValidators(Validators.required);
        motivoControl.setValue('');
      }
      motivoControl.updateValueAndValidity();
    });

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
        this.listaNegraForm.patchValue({
          enListaNegra: paciente.enListaNegra,
          motivo: paciente.motivoListaNegra || ''
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
    this.listaNegraForm.markAllAsTouched();

    if (this.listaNegraForm.invalid) {
      return;
    }

    const valores = this.listaNegraForm.getRawValue();
    const request: ListaNegraRequest = {
      enListaNegra: valores.enListaNegra,
      motivo: valores.enListaNegra ? valores.motivo.trim() : null
    };

    this._guardando.set(true);

    this.pacienteService.actualizarListaNegra(this.pacienteId, request).subscribe({
      next: () => {
        this._guardando.set(false);
        this._mensaje.set('Lista negra actualizada correctamente.');
        setTimeout(() => this.router.navigate(['/pacientes', this.pacienteId]), 800);
      },
      error: (error) => {
        this._guardando.set(false);
        this._error.set(error.error?.mensaje || 'Error al actualizar la lista negra.');
      }
    });
  }
}
