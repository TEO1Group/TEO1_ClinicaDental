import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Observable } from 'rxjs';
import { NotificacionService } from '../../../core/notificacion/service/notificacion.service';
import {
  ReporteCitasPorDoctorResponse,
  ReporteGananciasResponse,
  ReporteHorasDemandadasResponse,
  ReporteInasistenciasResponse,
  ReporteIngresosTratamientoResponse
} from '../../models/reporte-citas.model';
import { ReporteService } from '../../service/reporte.service';

function obtenerFechaHoyGuatemala(): string {
  const partes = new Intl.DateTimeFormat('en-US', {
    timeZone: 'America/Guatemala',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  }).formatToParts(new Date());
  const ano = partes.find(parte => parte.type === 'year')?.value;
  const mes = partes.find(parte => parte.type === 'month')?.value;
  const dia = partes.find(parte => parte.type === 'day')?.value;

  return `${ano}-${mes}-${dia}`;
}

type TipoReporte = 'citasPorDoctor' | 'inasistencias' | 'ingresosPorTratamiento' | 'horasDemandadas' | 'ganancias';
type ReporteResponse =
  | ReporteCitasPorDoctorResponse
  | ReporteInasistenciasResponse
  | ReporteIngresosTratamientoResponse
  | ReporteHorasDemandadasResponse
  | ReporteGananciasResponse;

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

  readonly fechaHoy = obtenerFechaHoyGuatemala();

  readonly filtroForm = this.formBuilder.nonNullable.group({
    desde: ['', [Validators.required]],
    hasta: ['', [Validators.required]]
  });

  readonly tipoReporte = signal<TipoReporte>('citasPorDoctor');
  private readonly _reporteCitas = signal<ReporteCitasPorDoctorResponse | null>(null);
  private readonly _reporteInasistencias = signal<ReporteInasistenciasResponse | null>(null);
  private readonly _reporteIngresos = signal<ReporteIngresosTratamientoResponse | null>(null);
  private readonly _reporteHoras = signal<ReporteHorasDemandadasResponse | null>(null);
  private readonly _reporteGanancias = signal<ReporteGananciasResponse | null>(null);
  private readonly _cargando = signal(false);
  private readonly _error = signal('');

  readonly reporteCitas = this._reporteCitas.asReadonly();
  readonly reporteInasistencias = this._reporteInasistencias.asReadonly();
  readonly reporteIngresos = this._reporteIngresos.asReadonly();
  readonly reporteHoras = this._reporteHoras.asReadonly();
  readonly reporteGanancias = this._reporteGanancias.asReadonly();
  readonly cargando = this._cargando.asReadonly();
  readonly error = this._error.asReadonly();

  ngOnInit(): void {
    const hoyGuatemala = this.fechaHoy;
    const [ano, mes] = hoyGuatemala.split('-').map(Number);
    const ultimoDiaMes = new Date(ano, mes, 0).getDate();
    const finDeMes = `${ano}-${String(mes).padStart(2, '0')}-${String(ultimoDiaMes).padStart(2, '0')}`;
    const hastaPorDefecto = hoyGuatemala < finDeMes ? hoyGuatemala : finDeMes;

    this.filtroForm.setValue({
      desde: `${ano}-${String(mes).padStart(2, '0')}-01`,
      hasta: hastaPorDefecto
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

    if (hasta > this.fechaHoy) {
      this._error.set('La fecha final no puede ser posterior a la fecha de hoy.');
      return;
    }

    this._cargando.set(true);
    const solicitud: Observable<ReporteResponse> = {
      citasPorDoctor: () => this.reporteService.citasPorDoctor(desde, hasta),
      inasistencias: () => this.reporteService.inasistencias(desde, hasta),
      ingresosPorTratamiento: () => this.reporteService.ingresosPorTratamiento(desde, hasta),
      horasDemandadas: () => this.reporteService.horasDemandadas(desde, hasta),
      ganancias: () => this.reporteService.ganancias(desde, hasta)
    }[this.tipoReporte()]();

    solicitud.subscribe({
      next: (reporte) => {
        this.limpiarReportes();
        if (this.tipoReporte() === 'citasPorDoctor') this._reporteCitas.set(reporte as ReporteCitasPorDoctorResponse);
        if (this.tipoReporte() === 'inasistencias') this._reporteInasistencias.set(reporte as ReporteInasistenciasResponse);
        if (this.tipoReporte() === 'ingresosPorTratamiento') this._reporteIngresos.set(reporte as ReporteIngresosTratamientoResponse);
        if (this.tipoReporte() === 'horasDemandadas') this._reporteHoras.set(reporte as ReporteHorasDemandadasResponse);
        if (this.tipoReporte() === 'ganancias') this._reporteGanancias.set(reporte as ReporteGananciasResponse);
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
    this._reporteGanancias.set(null);
  }
}
