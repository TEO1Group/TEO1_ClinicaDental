import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { toSignal } from '@angular/core/rxjs-interop';
import { PacienteService } from '../models/service/paciente.service';
import { ClienteResponse } from '../models/cliente.model';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-listado-pacientes',
  standalone: true,
  imports: [CommonModule, RouterLink, ReactiveFormsModule],
  templateUrl: './listado-pacientes.component.html',
  styleUrl: './listado-pacientes.component.scss'
})
export class ListadoPacientesComponent implements OnInit {
  private readonly pacienteService = inject(PacienteService);
  private readonly authService = inject(AuthService);
  private readonly formBuilder = inject(FormBuilder);

  readonly rol = this.authService.rol;

  readonly filtroListaNegra = this.formBuilder.nonNullable.group({
    listaNegra: ['']
  });

  private readonly filtroListaNegraValor = toSignal(
    this.filtroListaNegra.controls.listaNegra.valueChanges,
    { initialValue: '' }
  );

  private readonly _pacientes = signal<ClienteResponse[]>([]);
  private readonly _cargando = signal<boolean>(false);
  private readonly _error = signal<string>('');

  readonly pacientes = this._pacientes.asReadonly();
  readonly cargando = this._cargando.asReadonly();
  readonly error = this._error.asReadonly();

  readonly pacientesFiltrados = computed(() => {
    const filtro = this.filtroListaNegraValor();
    const lista = this._pacientes();

    if (filtro === 'enListaNegra') {
      return lista.filter(p => p.enListaNegra);
    }
    if (filtro === 'noListaNegra') {
      return lista.filter(p => !p.enListaNegra);
    }
    return lista;
  });

  ngOnInit(): void {
    this.cargarPacientes();
  }

  cargarPacientes(): void {
    this._cargando.set(true);
    this._error.set('');

    this.pacienteService.listarPacientes().subscribe({
      next: (response) => {
        this._pacientes.set(response);
        this._cargando.set(false);
      },
      error: (error) => {
        this._cargando.set(false);
        this._error.set(error.error?.mensaje || 'Error al cargar los pacientes.');
      }
    });
  }

  puedeCrear(): boolean {
    const rol = this.rol();
    return rol === 'ADMIN' || rol === 'SECRETARIA';
  }
}