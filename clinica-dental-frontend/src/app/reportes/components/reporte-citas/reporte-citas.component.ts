import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NotificacionService } from '../../../core/notificacion/service/notificacion.service';
import {
  ReporteCitasPorDoctorResponse,
  ReporteHorasDemandadasResponse,
  ReporteInasistenciasResponse,
  ReporteIngresosTratamientoResponse
} from '../../models/reporte-citas.model';
import { ReporteService } from '../../service/reporte.service';

type TipoReporte = 'citasPorDoctor' | 'inasistencias' | 'ingresosPorTratamiento' | 'horasDemandadas';

@Component({
  selector: 'app-reporte-citas',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './reporte-citas.component.html',
  styleUrl: './reporte-citas.component.scss'
})
export class ReporteCitasComponent implements OnInit {
  private readonly formBuilder = inject(FormBuilder);
  private readonly reporteService = inject(ReporteService);
  private readonly notificacionService = inject(NotificacionService);

  readonly filtroForm = this.formBuilder.nonNullable.group({
    desde: ['', [Validators.required]],
    hasta: ['', [Validators.required]]
  });

  readonly tipoReporte = signal<TipoReporte>('citasPorDoctor');
  private readonly _reporteCitas = signal<ReporteCitasPorDoctorResponse | null>(null);
  private readonly _reporteInasistencias = signal<ReporteInasistenciasResponse | null>(null);
  private readonly _reporteIngresos = signal<ReporteIngresosTratamientoResponse | null>(null);
  private readonly _reporteHoras = signal<ReporteHorasDemandadasResponse | null>(null);
  private readonly _cargando = signal(false);
  private readonly _error = signal('');

  readonly reporteCitas = this._reporteCitas.asReadonly();
  readonly reporteInasistencias = this._reporteInasistencias.asReadonly();
  readonly reporteIngresos = this._reporteIngresos.asReadonly();
  readonly reporteHoras = this._reporteHoras.asReadonly();
  readonly cargando = this._cargando.asReadonly();
  readonly error = this._error.asReadonly();

  ngOnInit(): void {
    const hoy = new Date();
    this.filtroForm.setValue({
      desde: this.formatearFecha(new Date(hoy.getFullYear(), hoy.getMonth(), 1)),
      hasta: this.formatearFecha(new Date(hoy.getFullYear(), hoy.getMonth() + 1, 0))
    });
    this.cargarReporte();
  }

  cargarReporte(): void {
    this._error.set('');

    if (this.filtroForm.invalid) {
      this.filtroForm.markAllAsTouched();
      return;
    }

    const { desde, hasta } = this.filtroForm.getRawValue();
    if (desde > hasta) {
      this._error.set('La fecha inicial no puede ser posterior a la fecha final.');
      return;
    }

    this._cargando.set(true);
    const solicitud = {
      citasPorDoctor: () => this.reporteService.citasPorDoctor(desde, hasta),
      inasistencias: () => this.reporteService.inasistencias(desde, hasta),
      ingresosPorTratamiento: () => this.reporteService.ingresosPorTratamiento(desde, hasta),
      horasDemandadas: () => this.reporteService.horasDemandadas(desde, hasta)
    }[this.tipoReporte()]();

    solicitud.subscribe({
      next: (reporte) => {
        this.limpiarReportes();
        if (this.tipoReporte() === 'citasPorDoctor') this._reporteCitas.set(reporte as ReporteCitasPorDoctorResponse);
        if (this.tipoReporte() === 'inasistencias') this._reporteInasistencias.set(reporte as ReporteInasistenciasResponse);
        if (this.tipoReporte() === 'ingresosPorTratamiento') this._reporteIngresos.set(reporte as ReporteIngresosTratamientoResponse);
        if (this.tipoReporte() === 'horasDemandadas') this._reporteHoras.set(reporte as ReporteHorasDemandadasResponse);
        this._cargando.set(false);
      },
      error: (error) => {
        this._cargando.set(false);
        this._error.set(error.error?.mensaje || 'Error al cargar el reporte.');
        this.notificacionService.error(this._error());
      }
    });
  }

  seleccionarReporte(tipo: string): void {
    this.tipoReporte.set(tipo as TipoReporte);
    this.cargarReporte();
  }

  totalAgendadas(): number {
    return this.reporteCitas()?.doctores.reduce((total, doctor) => total + doctor.agendadas, 0) || 0;
  }

  totalAtendidas(): number {
    return this.reporteCitas()?.doctores.reduce((total, doctor) => total + doctor.atendidas, 0) || 0;
  }

  totalCanceladas(): number {
    return this.reporteCitas()?.doctores.reduce((total, doctor) => total + doctor.canceladas, 0) || 0;
  }

  totalNoAsistio(): number {
    return this.reporteCitas()?.doctores.reduce((total, doctor) => total + doctor.noAsistio, 0) || 0;
  }

  totalCitas(): number {
    return this.reporteCitas()?.doctores.reduce((total, doctor) => total + doctor.total, 0) || 0;
  }

  sumarCitas(total: number, hora: { cantidad: number }): number {
    return total + hora.cantidad;
  }

  private limpiarReportes(): void {
    this._reporteCitas.set(null);
    this._reporteInasistencias.set(null);
    this._reporteIngresos.set(null);
    this._reporteHoras.set(null);
  }

  private formatearFecha(fecha: Date): string {
    const mes = String(fecha.getMonth() + 1).padStart(2, '0');
    const dia = String(fecha.getDate()).padStart(2, '0');
    return `${fecha.getFullYear()}-${mes}-${dia}`;
  }
}