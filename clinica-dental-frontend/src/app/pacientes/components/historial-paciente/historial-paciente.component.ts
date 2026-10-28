import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { NotificacionService } from '../../../core/notificacion/service/notificacion.service';
import { AuthService } from '../../../core/services/auth.service';
import { CitaResponse } from '../../../citas/models/cita.model';
import { CitaService } from '../../../citas/service/cita.service';
import { HistorialClinicoResponse } from '../../models/historial-clinico.model';
import { CitaTratamientoRequest, CitaTratamientoResponse, TratamientoResponse } from '../../../tratamientos-paciente/models/tratamiento.model';
import { PacienteService } from '../../service/paciente.service';
import { TratamientoService } from '../../../tratamientos-paciente/service/tratamiento.service';

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
  private readonly citaService = inject(CitaService);
  private readonly tratamientoService = inject(TratamientoService);
  private readonly authService = inject(AuthService);
  private readonly notificacionService = inject(NotificacionService);
  private readonly formBuilder = inject(FormBuilder);

  readonly rol = this.authService.rol;
  readonly historialForm = this.formBuilder.nonNullable.group({
    descripcion: ['', [Validators.required]]
  });
  readonly tratamientoForm = this.formBuilder.nonNullable.group({
    idCita: ['', [Validators.required]],
    idTratamiento: ['', [Validators.required]],
    costo: ['', [Validators.pattern(/^\d+(\.\d{1,2})?$/), Validators.min(0)]],
    observaciones: ['', [Validators.maxLength(1000)]]
  });

  pacienteId = '';

  private readonly _nombrePaciente = signal('');
  private readonly _historial = signal<HistorialClinicoResponse[]>([]);
  private readonly _cargando = signal(false);
  private readonly _guardando = signal(false);
  private readonly _error = signal('');
  private readonly _errorFormulario = signal('');
  private readonly _mensaje = signal('');
  private readonly _tratamientos = signal<CitaTratamientoResponse[]>([]);
  private readonly _totalTratamientos = signal(0);
  private readonly _citasAtendidas = signal<CitaResponse[]>([]);
  private readonly _catalogoTratamientos = signal<TratamientoResponse[]>([]);
  private readonly _cargandoTratamientos = signal(false);
  private readonly _cargandoFormularioTratamiento = signal(false);

  readonly nombrePaciente = this._nombrePaciente.asReadonly();
  readonly historial = this._historial.asReadonly();
  readonly cargando = this._cargando.asReadonly();
  readonly guardando = this._guardando.asReadonly();
  readonly error = this._error.asReadonly();
  readonly errorFormulario = this._errorFormulario.asReadonly();
  readonly mensaje = this._mensaje.asReadonly();
  readonly tratamientos = this._tratamientos.asReadonly();
  readonly totalTratamientos = this._totalTratamientos.asReadonly();
  readonly citasAtendidas = this._citasAtendidas.asReadonly();
  readonly catalogoTratamientos = this._catalogoTratamientos.asReadonly();
  readonly cargandoTratamientos = this._cargandoTratamientos.asReadonly();
  readonly cargandoFormularioTratamiento = this._cargandoFormularioTratamiento.asReadonly();

  ngOnInit(): void {
    this.pacienteId = this.route.snapshot.paramMap.get('id') || '';

    if (this.pacienteId) {
      this.cargarPaciente();
      this.cargarHistorial();
      this.cargarTratamientos();
      if (this.puedeRegistrarTratamiento()) {
        this.cargarDatosFormularioTratamiento();
      }
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

  puedeRegistrarTratamiento(): boolean {
    return this.rol() === 'DOCTOR';
  }

  cargarTratamientos(): void {
    this._cargandoTratamientos.set(true);

    this.tratamientoService.listarPorPaciente(this.pacienteId).subscribe({
      next: (respuesta) => {
        this._tratamientos.set(respuesta.tratamientos);
        this._totalTratamientos.set(respuesta.total);
        this._cargandoTratamientos.set(false);
      },
      error: (error) => {
        this._cargandoTratamientos.set(false);
        this.notificacionService.error(error.error?.mensaje || 'Error al cargar los tratamientos.');
      }
    });
  }

  cargarDatosFormularioTratamiento(): void {
    this._cargandoFormularioTratamiento.set(true);

    this.citaService.listar({ idCliente: this.pacienteId, estado: 'ATENDIDA' }).subscribe({
      next: (citas) => {
        this._citasAtendidas.set(citas);
        this.cargarCatalogoTratamientos();
      },
      error: (error) => {
        this._cargandoFormularioTratamiento.set(false);
        this.notificacionService.error(error.error?.mensaje || 'Error al cargar las citas atendidas.');
      }
    });
  }

  private cargarCatalogoTratamientos(): void {
    this.tratamientoService.listar().subscribe({
      next: (tratamientos) => {
        this._catalogoTratamientos.set(tratamientos);
        this._cargandoFormularioTratamiento.set(false);
      },
      error: (error) => {
        this._cargandoFormularioTratamiento.set(false);
        this.notificacionService.error(error.error?.mensaje || 'Error al cargar el catálogo de tratamientos.');
      }
    });
  }

  registrarTratamiento(): void {
    this.tratamientoForm.markAllAsTouched();
    if (this.tratamientoForm.invalid) {
      return;
    }

    const valores = this.tratamientoForm.getRawValue();
    const request: CitaTratamientoRequest = {
      idTratamiento: valores.idTratamiento,
      costo: valores.costo ? Number(valores.costo) : undefined,
      observaciones: valores.observaciones || undefined
    };

    this._guardando.set(true);
    this.citaService.registrarTratamiento(valores.idCita, request).subscribe({
      next: () => {
        this._guardando.set(false);
        this.tratamientoForm.reset();
        this.cargarTratamientos();
        this.notificacionService.exito('Tratamiento registrado correctamente.');
      },
      error: (error) => {
        this._guardando.set(false);
        this.notificacionService.error(error.error?.mensaje || 'Error al registrar el tratamiento.');
      }
    });
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
