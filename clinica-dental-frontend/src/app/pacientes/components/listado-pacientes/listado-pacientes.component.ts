import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { AuthService } from '../../../core/services/auth.service';
import { ClienteResponse } from '../../models/paciente.model';
import { PacienteService } from '../../service/paciente.service';

@Component({
  selector: 'app-listado-pacientes',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './listado-pacientes.component.html',
  styleUrl: './listado-pacientes.component.scss'
})
export class ListadoPacientesComponent implements OnInit {
  private readonly pacienteService = inject(PacienteService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly authService = inject(AuthService);

  readonly rol = this.authService.rol;
  readonly filtroForm = this.formBuilder.nonNullable.group({
    listaNegra: ['']
  });

  private readonly listaNegraFiltro = toSignal(
    this.filtroForm.controls.listaNegra.valueChanges,
    { initialValue: '' }
  );

  private readonly _pacientes = signal<ClienteResponse[]>([]);
  private readonly _cargando = signal(false);
  private readonly _error = signal('');

  readonly pacientes = this._pacientes.asReadonly();
  readonly cargando = this._cargando.asReadonly();
  readonly error = this._error.asReadonly();

  readonly pacientesFiltrados = computed(() => {
    const filtro = this.listaNegraFiltro();
    const pacientes = this._pacientes();

    if (filtro === '') {
      return pacientes;
    }

    const enListaNegra = filtro === 'true';
    return pacientes.filter(paciente => paciente.enListaNegra === enListaNegra);
  });

  ngOnInit(): void {
    this.cargarPacientes();
  }

  cargarPacientes(): void {
    this._cargando.set(true);
    this._error.set('');

    this.pacienteService.listarPacientes().subscribe({
      next: (pacientes) => {
        this._pacientes.set(pacientes);
        this._cargando.set(false);
      },
      error: (error) => {
        this._cargando.set(false);
        this._error.set(error.error?.mensaje || 'Error al cargar los pacientes.');
      }
    });
  }

  puedeEditar(): boolean {
    return this.rol() === 'ADMIN' || this.rol() === 'SECRETARIA';
  }

  puedeGestionarListaNegra(): boolean {
    return this.rol() === 'SECRETARIA';
  }

  puedeAgregarHistorial(): boolean {
    return this.rol() === 'ADMIN' || this.rol() === 'DOCTOR';
  }
}
