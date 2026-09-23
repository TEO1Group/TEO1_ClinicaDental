import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { DoctorService } from '../service/doctor.service';
import { AuthService } from '../../core/services/auth.service';
import { DoctorResponse } from '../models/doctor.model';
import { HorarioResponse } from '../models/horario.mode';

@Component({
  selector: 'app-detalle-doctor',
  standalone: true,
  imports: [CommonModule, RouterLink, ReactiveFormsModule],
  templateUrl: './detalle-doctor.component.html',
  styleUrl: './detalle-doctor.component.scss'
})
export class DetalleDoctorComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly doctorService = inject(DoctorService);
  private readonly authService = inject(AuthService);
  private readonly formBuilder = inject(FormBuilder);

  readonly rol = this.authService.rol;

  readonly diasSemana = [
    { valor: 'LUNES', etiqueta: 'Lunes' },
    { valor: 'MARTES', etiqueta: 'Martes' },
    { valor: 'MIERCOLES', etiqueta: 'Miércoles' },
    { valor: 'JUEVES', etiqueta: 'Jueves' },
    { valor: 'VIERNES', etiqueta: 'Viernes' },
    { valor: 'SABADO', etiqueta: 'Sábado' },
    { valor: 'DOMINGO', etiqueta: 'Domingo' }
  ];

  private readonly _doctor = signal<DoctorResponse | null>(null);
  private readonly _horarios = signal<HorarioResponse[]>([]);
  private readonly _cargando = signal<boolean>(false);
  private readonly _error = signal<string>('');

  readonly doctor = this._doctor.asReadonly();
  readonly horarios = this._horarios.asReadonly();
  readonly cargando = this._cargando.asReadonly();
  readonly error = this._error.asReadonly();

  // Modal edición
  mostrarModalEditar = false;
  isSubmittingEditar = false;
  successEditar = '';
  errorEditar = '';

  readonly editarForm = this.formBuilder.nonNullable.group({
    especialidad: ['', [Validators.required]],
    numeroColegiado: [''],
    numeroClinica: ['']
  });

  // Agregar horario
  mostrarFormHorario = false;
  isSubmittingHorario = false;
  successHorario = '';
  errorHorario = '';

  readonly horarioForm = this.formBuilder.nonNullable.group({
    diaSemana: ['', [Validators.required]],
    horaInicio: ['', [Validators.required]],
    horaFin: ['', [Validators.required]]
  });

  private doctorId = '';

  ngOnInit(): void {
    this.doctorId = this.route.snapshot.paramMap.get('id') || '';
    if (this.doctorId) {
      this.cargarDoctor();
      this.cargarHorarios();
    }
  }

  cargarDoctor(): void {
    this._cargando.set(true);
    this._error.set('');

    this.doctorService.obtenerDoctor(this.doctorId).subscribe({
      next: (response) => {
        this._doctor.set(response);
        this._cargando.set(false);
      },
      error: (error) => {
        this._cargando.set(false);
        this._error.set(error.error?.mensaje || 'Error al cargar el doctor.');
      }
    });
  }

  cargarHorarios(): void {
    this.doctorService.listarHorarios(this.doctorId).subscribe({
      next: (response) => {
        this._horarios.set(response);
      },
      error: () => {
        // Silencioso: si fallan los horarios, la lista queda vacía
      }
    });
  }

  puedeEditar(): boolean {
    return this.rol() === 'ADMIN';
  }

  puedeAgregarHorario(): boolean {
    const rol = this.rol();
    return rol === 'ADMIN' || rol === 'DOCTOR';
  }

  abrirModalEditar(): void {
    const doc = this._doctor();
    if (!doc) return;

    this.editarForm.patchValue({
      especialidad: doc.especialidad,
      numeroColegiado: doc.numeroColegiado || '',
      numeroClinica: doc.numeroClinica || ''
    });

    this.successEditar = '';
    this.errorEditar = '';
    this.mostrarModalEditar = true;
  }

  cerrarModalEditar(): void {
    this.mostrarModalEditar = false;
    this.editarForm.reset();
  }

  guardarEdicion(): void {
    this.successEditar = '';
    this.errorEditar = '';

    if (this.editarForm.invalid) {
      this.editarForm.markAllAsTouched();
      return;
    }

    this.isSubmittingEditar = true;

    this.doctorService.actualizarDoctor(this.doctorId, this.editarForm.getRawValue()).subscribe({
      next: (response) => {
        this.isSubmittingEditar = false;
        this._doctor.set(response);
        this.successEditar = 'Doctor actualizado correctamente.';
        setTimeout(() => {
          this.cerrarModalEditar();
        }, 1500);
      },
      error: (error) => {
        this.isSubmittingEditar = false;
        this.errorEditar = error.error?.mensaje || 'Error al actualizar el doctor.';
      }
    });
  }

  toggleFormHorario(): void {
    this.mostrarFormHorario = !this.mostrarFormHorario;
    this.successHorario = '';
    this.errorHorario = '';
    if (!this.mostrarFormHorario) {
      this.horarioForm.reset();
    }
  }

  guardarHorario(): void {
    this.successHorario = '';
    this.errorHorario = '';

    if (this.horarioForm.invalid) {
      this.horarioForm.markAllAsTouched();
      return;
    }

    this.isSubmittingHorario = true;

    this.doctorService.agregarHorario(this.doctorId, this.horarioForm.getRawValue()).subscribe({
      next: (response) => {
        this.isSubmittingHorario = false;
        this._horarios.update(h => [...h, response]);
        this.successHorario = 'Horario agregado correctamente.';
        this.horarioForm.reset();
        setTimeout(() => {
          this.mostrarFormHorario = false;
          this.successHorario = '';
        }, 1500);
      },
      error: (error) => {
        this.isSubmittingHorario = false;
        this.errorHorario = error.error?.mensaje || 'Error al agregar el horario.';
      }
    });
  }

  formatearDia(dia: string): string {
    const encontrado = this.diasSemana.find(d => d.valor === dia);
    return encontrado?.etiqueta || dia;
  }

  formatearHora(hora: string): string {
    return hora.substring(0, 5);
   }
}