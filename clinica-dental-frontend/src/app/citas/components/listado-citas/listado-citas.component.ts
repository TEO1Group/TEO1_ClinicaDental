import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { toSignal } from '@angular/core/rxjs-interop';
import { NotificacionService } from '../../../core/notificacion/service/notificacion.service';
import { AuthService } from '../../../core/services/auth.service';
import { BloqueoVigenteResponse } from '../../../doctores/models/bloqueo.model';
import { DoctorService } from '../../../doctores/service/doctor.service';
import { CitaResponse, EstadoCita, EstadoCitaRequest } from '../../models/cita.model';
import { CitaService } from '../../service/cita.service';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-listado-citas',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './listado-citas.component.html',
  styleUrl: './listado-citas.component.scss'
})
export class ListadoCitasComponent implements OnInit {
  private readonly citaService = inject(CitaService);
  private readonly formBuilder = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly notificacionService = inject(NotificacionService);
  private readonly doctorService = inject(DoctorService);

  readonly rol = this.authService.rol;
  readonly usuarioActual = this.authService.usuarioActual;

  readonly filtroForm = this.formBuilder.nonNullable.group({
    estado: [''],
    fecha: ['']
  });

  private readonly estadoFiltro = toSignal(this.filtroForm.controls.estado.valueChanges, { initialValue: '' });
  private readonly fechaFiltro = toSignal(this.filtroForm.controls.fecha.valueChanges, { initialValue: '' });

  private readonly _citas = signal<CitaResponse[]>([]);
  private readonly _cargando = signal(false);
  private readonly _error = signal('');

  readonly citas = this._citas.asReadonly();
  readonly cargando = this._cargando.asReadonly();
  readonly error = this._error.asReadonly();

  readonly citasFiltradas = computed(() => {
    const estado = this.estadoFiltro();
    const fecha = this.fechaFiltro();

    return this._citas().filter(cita =>
      (!estado || cita.estado === estado) && (!fecha || cita.fecha === fecha)
    );
  });

  // Modal de motivo
  readonly motivoModalAbierto = signal(false);
  readonly motivoModalTexto = signal('');
  readonly motivoModalEstado = signal<EstadoCita | null>(null);
  readonly motivoModalCita = signal<CitaResponse | null>(null);
  readonly motivoModalError = signal('');

  // Modal de reprogramar
  readonly reprogramarModalAbierto = signal(false);
  readonly reprogramarFecha = signal('');
  readonly reprogramarHora = signal('');
  readonly reprogramarMotivo = signal('');
  readonly reprogramarCita = signal<CitaResponse | null>(null);
  readonly reprogramarError = signal('');
  readonly reprogramarEnviando = signal(false);
  readonly reprogramarBloqueos = signal<BloqueoVigenteResponse[]>([]);
  readonly reprogramarBloqueo = signal<BloqueoVigenteResponse | null>(null);

  ngOnInit(): void {
    this.cargarCitas();
  }

  cargarCitas(): void {
    this._cargando.set(true);
    this._error.set('');

    this.citaService.listarCitas().subscribe({
      next: (citas) => {
        this._citas.set(citas);
        this._cargando.set(false);
      },
      error: (error) => {
        this._cargando.set(false);
        if (error.status === 403) return;
        this._error.set(error.error?.mensaje || 'Error al cargar las citas.');
      }
    });
  }

  puedeCrear(): boolean {
    const rol = this.rol();
    return rol === 'CLIENTE' || rol === 'SECRETARIA' || rol === 'ADMIN';
  }

  puedeCancelar(cita: CitaResponse): boolean {
    return cita.estado === 'AGENDADA' && ['CLIENTE', 'SECRETARIA', 'ADMIN'].includes(this.rol() || '');
  }

  puedeMarcarAtendida(cita: CitaResponse): boolean {
    return cita.estado === 'AGENDADA' && ['DOCTOR', 'SECRETARIA', 'ADMIN'].includes(this.rol() || '');
  }

  puedeMarcarNoAsistio(cita: CitaResponse): boolean {
    return cita.estado === 'AGENDADA' && ['DOCTOR', 'SECRETARIA', 'ADMIN'].includes(this.rol() || '');
  }

  puedeReprogramar(cita: CitaResponse): boolean {
    if (cita.estado !== 'AGENDADA') return false;

    const rol = this.rol();
    const usuario = this.usuarioActual();

    if (rol === 'ADMIN' || rol === 'SECRETARIA') return true;
    if (rol === 'CLIENTE') return usuario?.idPerfil === cita.idCliente;

    return false;
  }

  // Modal de motivo
  abrirModalMotivo(cita: CitaResponse, estado: EstadoCita): void {
    this.motivoModalCita.set(cita);
    this.motivoModalEstado.set(estado);
    this.motivoModalTexto.set('');
    this.motivoModalError.set('');
    this.motivoModalAbierto.set(true);
  }

  cerrarModalMotivo(): void {
    this.motivoModalAbierto.set(false);
    this.motivoModalCita.set(null);
    this.motivoModalEstado.set(null);
    this.motivoModalTexto.set('');
    this.motivoModalError.set('');
  }

  esMotivoObligatorio(estado: EstadoCita): boolean {
    return estado === 'CANCELADA' || estado === 'NO_ASISTIO';
  }

  confirmarCambioEstado(): void {
    const cita = this.motivoModalCita();
    const estado = this.motivoModalEstado();
    const motivo = this.motivoModalTexto().trim();

    if (!cita || !estado) return;

    if (this.esMotivoObligatorio(estado) && !motivo) {
      this.motivoModalError.set('El motivo es obligatorio para este estado.');
      return;
    }

    const request: EstadoCitaRequest = {
      estado,
      motivo: motivo || undefined
    };

    this.citaService.cambiarEstado(cita.idCita, request).subscribe({
      next: () => {
        this.notificacionService.exito('Estado de la cita actualizado correctamente.');
        this.cerrarModalMotivo();
        this.cargarCitas();
        this.recargarCampanaSiAplica();
      },
      error: (error) => {
        if (error.status === 403) return;
        this.notificacionService.error(error.error?.mensaje || 'Error al cambiar el estado de la cita.');
      }
    });
  }

  // Modal de reprogramar
  abrirModalReprogramar(cita: CitaResponse): void {
    this.reprogramarCita.set(cita);
    this.reprogramarFecha.set(cita.fecha);
    this.reprogramarHora.set(cita.hora.slice(0, 5));
    this.reprogramarMotivo.set('');
    this.reprogramarError.set('');
    this.reprogramarBloqueos.set([]);
    this.reprogramarBloqueo.set(null);
    this.reprogramarModalAbierto.set(true);
    this.cargarBloqueosReprogramar(cita.idDoctor);
  }

  cerrarModalReprogramar(): void {
    this.reprogramarModalAbierto.set(false);
    this.reprogramarCita.set(null);
    this.reprogramarFecha.set('');
    this.reprogramarHora.set('');
    this.reprogramarMotivo.set('');
    this.reprogramarError.set('');
    this.reprogramarBloqueos.set([]);
    this.reprogramarBloqueo.set(null);
  }

  actualizarReprogramarFecha(fecha: string): void {
    this.reprogramarFecha.set(fecha);
    this.actualizarBloqueoReprogramar();
  }

  actualizarReprogramarHora(hora: string): void {
    this.reprogramarHora.set(hora);
    this.actualizarBloqueoReprogramar();
  }

  cargarBloqueosReprogramar(idDoctor: string): void {
    this.doctorService.listarBloqueosVigentes(idDoctor).subscribe({
      next: (bloqueos) => {
        this.reprogramarBloqueos.set(bloqueos);
        this.actualizarBloqueoReprogramar();
      },
      error: (error) => {
        this.reprogramarError.set(error.error?.mensaje || 'Error al cargar los bloqueos del doctor.');
      }
    });
  }

  confirmarReprogramar(): void {
    const cita = this.reprogramarCita();
    const fecha = this.reprogramarFecha().trim();
    const hora = this.reprogramarHora().trim();
    const motivo = this.reprogramarMotivo().trim();

    if (!cita) return;

    if (!fecha || !hora) {
      this.reprogramarError.set('La fecha y hora son obligatorias.');
      return;
    }

    if (!motivo) {
      this.reprogramarError.set('El motivo es obligatorio.');
      return;
    }

    if (this.reprogramarBloqueo()) {
      return;
    }

    if (motivo.length > 200) {
      this.reprogramarError.set('El motivo no puede superar 200 caracteres.');
      return;
    }

    this.reprogramarEnviando.set(true);
    this.reprogramarError.set('');

    this.citaService.reprogramar(cita.idCita, { fecha, hora, motivo }).subscribe({
      next: () => {
        this.reprogramarEnviando.set(false);
        this.notificacionService.exito('Cita reprogramada correctamente.');
        this.cerrarModalReprogramar();
        this.cargarCitas();
        this.recargarCampanaSiAplica();
      },
      error: (error) => {
        this.reprogramarEnviando.set(false);
        if (error.status === 403) return;
        this.reprogramarError.set(error.error?.mensaje || 'Error al reprogramar la cita.');
      }
    });
  }

  private recargarCampanaSiAplica(): void {
    const rol = this.rol();
    if (rol === 'CLIENTE' || rol === 'DOCTOR') {
      this.citaService.recargarProximas();
    }
  }

  private actualizarBloqueoReprogramar(): void {
    const fecha = this.reprogramarFecha();
    const hora = this.reprogramarHora();
    if (!fecha || !hora) {
      this.reprogramarBloqueo.set(null);
      return;
    }

    const inicioCita = `${fecha}T${hora}`;
    this.reprogramarBloqueo.set(this.reprogramarBloqueos().find(bloqueo => {
      const inicioBloqueo = bloqueo.inicio.slice(0, 16);
      const finBloqueo = bloqueo.fin.slice(0, 16);
      return inicioCita >= inicioBloqueo && inicioCita < finBloqueo;
    }) || null);
  }

  nombreDoctor(cita: CitaResponse): string {
    return [cita.nombreDoctor, cita.apellidoDoctor].filter(Boolean).join(' ') || cita.idDoctor;
  }

  nombrePaciente(cita: CitaResponse): string {
    return [cita.nombreCliente, cita.apellidoCliente].filter(Boolean).join(' ') || cita.idCliente;
  }

  etiquetaEstado(estado: EstadoCita): string {
    return {
      AGENDADA: 'Agendada',
      ATENDIDA: 'Atendida',
      CANCELADA: 'Cancelada',
      NO_ASISTIO: 'No asistió'
    }[estado];
  }

  claseEstado(estado: EstadoCita): string {
    return {
      AGENDADA: 'bg-primary',
      ATENDIDA: 'bg-success',
      CANCELADA: 'bg-danger',
      NO_ASISTIO: 'bg-warning text-dark'
    }[estado];
  }
}
