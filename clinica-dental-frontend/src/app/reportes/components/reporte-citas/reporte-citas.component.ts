import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { NotificacionService } from '../../../core/notificacion/service/notificacion.service';
import { ReporteCitasPorDoctorResponse } from '../../models/reporte-citas.model';
import { ReporteService } from '../../service/reporte.service';

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

  private readonly _reporte = signal<ReporteCitasPorDoctorResponse | null>(null);
  private readonly _cargando = signal(false);
  private readonly _error = signal('');

  readonly reporte = this._reporte.asReadonly();
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
    this.reporteService.citasPorDoctor(desde, hasta).subscribe({
      next: (reporte) => {
        this._reporte.set(reporte);
        this._cargando.set(false);
      },
      error: (error) => {
        this._cargando.set(false);
        this._error.set(error.error?.mensaje || 'Error al cargar el reporte.');
        this.notificacionService.error(this._error());
      }
    });
  }

  totalAgendadas(): number {
    return this.reporte()?.doctores.reduce((total, doctor) => total + doctor.agendadas, 0) || 0;
  }

  totalAtendidas(): number {
    return this.reporte()?.doctores.reduce((total, doctor) => total + doctor.atendidas, 0) || 0;
  }

  totalCanceladas(): number {
    return this.reporte()?.doctores.reduce((total, doctor) => total + doctor.canceladas, 0) || 0;
  }

  totalNoAsistio(): number {
    return this.reporte()?.doctores.reduce((total, doctor) => total + doctor.noAsistio, 0) || 0;
  }

  totalCitas(): number {
    return this.reporte()?.doctores.reduce((total, doctor) => total + doctor.total, 0) || 0;
  }

  private formatearFecha(fecha: Date): string {
    const mes = String(fecha.getMonth() + 1).padStart(2, '0');
    const dia = String(fecha.getDate()).padStart(2, '0');
    return `${fecha.getFullYear()}-${mes}-${dia}`;
  }
}