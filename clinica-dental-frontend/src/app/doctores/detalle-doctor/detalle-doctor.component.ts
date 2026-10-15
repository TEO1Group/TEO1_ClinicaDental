import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { DoctorService } from '../service/doctor.service';
import { AuthService } from '../../core/services/auth.service';
import { NotificacionService } from '../../core/notificacion/service/notificacion.service';
import { DoctorResponse } from '../models/doctor.model';
import { HorarioResponse } from '../models/horario.mode';
import { BloqueoDoctorResponse } from '../models/bloqueo.model';

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
  private readonly notificacionService = inject(NotificacionService);
  private readonly formBuilder = inject(FormBuilder);

  readonly rol = this.authService.rol;
  readonly usuarioActual = this.authService.usuarioActual;

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
  private readonly _bloqueos = signal<BloqueoDoctorResponse[]>([]);
  private readonly _cargando = signal<boolean>(false);
  private readonly _error = signal<string>('');

  readonly doctor = this._doctor.asReadonly();
  readonly horarios = this._horarios.asReadonly();
  readonly bloqueos = this._bloqueos.asReadonly();
  readonly cargando = this._cargando.asReadonly();
  readonly error = this._error.asReadonly();

  // Modal edición doctor
  mostrarModalEditar = false;
  isSubmittingEditar = false;

  readonly editarForm = this.formBuilder.nonNullable.group({
    nombre: ['', [Validators.maxLength(80)]],
    apellido: ['', [Validators.maxLength(80)]],
    telefono: ['', [Validators.pattern(/^[0-9]{8}$/)]],
    especialidad: ['', [Validators.required, Validators.maxLength(100)]],
    numeroColegiado: ['', [Validators.maxLength(30)]],
    numeroClinica: ['', [Validators.maxLength(20)]]
  });

  // Formulario agregar/editar horario
  mostrarFormHorario = false;
  isSubmittingHorario = false;
  horarioEditandoId: string | null = null;

  readonly horarioForm = this.formBuilder.nonNullable.group({
    diaSemana: ['', [Validators.required]],
    horaInicio: ['', [Validators.required]],
    horaFin: ['', [Validators.required]]
  });

  // Modal agregar bloqueo
  mostrarModalBloqueo = false;
  isSubmittingBloqueo = false;

  readonly bloqueoForm = this.formBuilder.nonNullable.group({
    inicio: ['', [Validators.required]],
    fin: ['', [Validators.required]],
    motivo: ['', [Validators.required, Validators.maxLength(200)]]
  });

  private doctorId = '';

  ngOnInit(): void {
    this.doctorId = this.route.snapshot.paramMap.get('id') || '';
    if (this.doctorId) {
      this.cargarDoctor();
      this.cargarHorarios();
      if (this.puedeVerBloqueos()) {
        this.cargarBloqueos();
      }
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
        if (error.status === 403) return;
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
        // Silencioso
      }
    });
  }

  cargarBloqueos(): void {
    this.doctorService.listarBloqueos(this.doctorId).subscribe({
      next: (response) => {
        this._bloqueos.set(response);
      },
      error: () => {
        // Silencioso
      }
    });
  }

  puedeEditar(): boolean {
    return this.rol() === 'ADMIN';
  }

  puedeDesactivar(): boolean {
    return this.rol() === 'ADMIN';
  }

  puedeGestionarHorarios(): boolean {
    const rolActual = this.rol();
    const usuario = this.usuarioActual();

    if (rolActual === 'ADMIN' || rolActual === 'SECRETARIA') {
      return true;
    }

    if (rolActual === 'DOCTOR') {
      return usuario?.idPerfil === this.doctorId;
    }

    return false;
  }

  puedeVerBloqueos(): boolean {
    const rolActual = this.rol();
    if (rolActual === 'ADMIN' || rolActual === 'SECRETARIA') {
      return true;
    }
    if (rolActual === 'DOCTOR') {
      return this.usuarioActual()?.idPerfil === this.doctorId;
    }
    return false;
  }

  puedeGestionarBloqueos(): boolean {
    const rolActual = this.rol();
    return rolActual === 'ADMIN' || rolActual === 'SECRETARIA';
  }

  // Modal editar doctor
  abrirModalEditar(): void {
    const doc = this._doctor();
    if (!doc) return;

    this.editarForm.patchValue({
      nombre: doc.nombre,
      apellido: doc.apellido,
      telefono: doc.telefono || '',
      especialidad: doc.especialidad,
      numeroColegiado: doc.numeroColegiado || '',
      numeroClinica: doc.numeroClinica || ''
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
      especialidad: valores.especialidad,
      numeroColegiado: valores.numeroColegiado || undefined,
      numeroClinica: valores.numeroClinica || undefined
    };

    this.doctorService.actualizarDoctor(this.doctorId, request).subscribe({
      next: (response) => {
        this.isSubmittingEditar = false;
        this._doctor.set(response);
        this.notificacionService.exito('Doctor actualizado correctamente.');
        this.cerrarModalEditar();
      },
      error: (error) => {
        if (error.status === 403) return;
        this.notificacionService.error(error.error?.mensaje || 'Error al actualizar el doctor.');
      }
    });
  }

  desactivarDoctor(): void {
    if (!confirm('¿Estás seguro de desactivar este doctor? Ya no podrá iniciar sesión.')) {
      return;
    }

    this.doctorService.desactivarDoctor(this.doctorId).subscribe({
      next: () => {
        this.notificacionService.exito('Doctor desactivado correctamente.');
        window.history.back();
      },
      error: (error) => {
        if (error.status === 403) return;
        this.notificacionService.error(error.error?.mensaje || 'Error al desactivar el doctor.');
      }
    });
  }

  // Horarios
  toggleFormHorario(): void {
    this.mostrarFormHorario = !this.mostrarFormHorario;
    this.horarioEditandoId = null;
    this.horarioForm.reset();
  }

  abrirFormEditarHorario(horario: HorarioResponse): void {
    this.horarioEditandoId = horario.idHorario;
    this.horarioForm.patchValue({
      diaSemana: horario.diaSemana,
      horaInicio: horario.horaInicio.slice(0, 5),
      horaFin: horario.horaFin.slice(0, 5)
    });
    this.mostrarFormHorario = true;
  }

  cancelarFormHorario(): void {
    this.mostrarFormHorario = false;
    this.horarioEditandoId = null;
    this.horarioForm.reset();
  }

  guardarHorario(): void {
    if (this.horarioForm.invalid) {
      this.horarioForm.markAllAsTouched();
      return;
    }

    this.isSubmittingHorario = true;

    const request = this.horarioForm.getRawValue();

    if (this.horarioEditandoId) {
      this.doctorService.actualizarHorario(this.doctorId, this.horarioEditandoId, request).subscribe({
        next: (response) => {
          this.isSubmittingHorario = false;
          this._horarios.update(h =>
            h.map(item => item.idHorario === response.idHorario ? response : item)
          );
          this.notificacionService.exito('Horario actualizado correctamente.');
          this.cancelarFormHorario();
        },
        error: (error) => {
          if (error.status === 403) return;
          this.notificacionService.error(error.error?.mensaje || 'Error al actualizar el horario.');
        }
      });
    } else {
      this.doctorService.agregarHorario(this.doctorId, request).subscribe({
        next: (response) => {
          this.isSubmittingHorario = false;
          this._horarios.update(h => [...h, response]);
          this.notificacionService.exito('Horario agregado correctamente.');
          this.cancelarFormHorario();
        },
        error: (error) => {
          if (error.status === 403) return;
          this.notificacionService.error(error.error?.mensaje || 'Error al agregar el horario.');
        }
      });
    }
  }

  eliminarHorario(horario: HorarioResponse): void {
    if (!confirm('¿Estás seguro de eliminar este horario?')) {
      return;
    }

    this.doctorService.eliminarHorario(this.doctorId, horario.idHorario).subscribe({
      next: () => {
        this._horarios.update(h => h.filter(item => item.idHorario !== horario.idHorario));
        this.notificacionService.exito('Horario eliminado correctamente.');
      },
      error: (error) => {
        if (error.status === 403) return;
        this.notificacionService.error(error.error?.mensaje || 'Error al eliminar el horario.');
      }
    });
  }

  formatearDia(dia: string): string {
    const encontrado = this.diasSemana.find(d => d.valor === dia);
    return encontrado?.etiqueta || dia;
  }

  // Bloqueos
  abrirModalBloqueo(): void {
    this.bloqueoForm.reset();
    this.mostrarModalBloqueo = true;
  }

  cerrarModalBloqueo(): void {
    this.mostrarModalBloqueo = false;
    this.bloqueoForm.reset();
  }

  guardarBloqueo(): void {
    if (this.bloqueoForm.invalid) {
      this.bloqueoForm.markAllAsTouched();
      return;
    }

    const valores = this.bloqueoForm.getRawValue();

    // Agregar :00 si falta (input datetime-local devuelve "yyyy-MM-ddTHH:mm")
    const inicio = valores.inicio.length === 16 ? `${valores.inicio}:00` : valores.inicio;
    const fin = valores.fin.length === 16 ? `${valores.fin}:00` : valores.fin;

    this.isSubmittingBloqueo = true;

    this.doctorService.crearBloqueo(this.doctorId, {
      inicio,
      fin,
      motivo: valores.motivo.trim()
    }).subscribe({
      next: (response) => {
        this.isSubmittingBloqueo = false;
        this._bloqueos.update(b => [...b, response]);
        this.notificacionService.exito('Bloqueo creado correctamente.');
        this.cerrarModalBloqueo();
      },
      error: (error) => {
        this.isSubmittingBloqueo = false;
        if (error.status === 403) return;
        this.notificacionService.error(error.error?.mensaje || 'Error al crear el bloqueo.');
      }
    });
  }

  eliminarBloqueo(bloqueo: BloqueoDoctorResponse): void {
    if (!confirm('¿Estás seguro de eliminar este bloqueo?')) {
      return;
    }

    this.doctorService.eliminarBloqueo(this.doctorId, bloqueo.idBloqueo).subscribe({
      next: () => {
        this._bloqueos.update(b => b.filter(item => item.idBloqueo !== bloqueo.idBloqueo));
        this.notificacionService.exito('Bloqueo eliminado correctamente.');
      },
      error: (error) => {
        if (error.status === 403) return;
        this.notificacionService.error(error.error?.mensaje || 'Error al eliminar el bloqueo.');
      }
    });
  }

  formatearFechaHora(fechaHora: string): string {
    // "2026-10-20T08:00:00" → "20/10/2026 08:00"
    const [fecha, hora] = fechaHora.split('T');
    const [year, month, day] = fecha.split('-');
    return `${day}/${month}/${year} ${hora.slice(0, 5)}`;
  }
}