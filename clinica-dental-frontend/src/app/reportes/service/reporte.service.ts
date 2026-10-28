import { HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../core/services/api.service';
import {
  ReporteCitasPorDoctorResponse,
  ReporteHorasDemandadasResponse,
  ReporteInasistenciasResponse,
  ReporteIngresosTratamientoResponse
} from '../models/reporte-citas.model';

@Injectable({ providedIn: 'root' })
export class ReporteService extends ApiService {
  private readonly reportesUrl = `${this.baseUrl}/reportes`;

  citasPorDoctor(desde: string, hasta: string): Observable<ReporteCitasPorDoctorResponse> {
    return this.obtener<ReporteCitasPorDoctorResponse>('citas-por-doctor', desde, hasta);
  }

  horasDemandadas(desde: string, hasta: string): Observable<ReporteHorasDemandadasResponse> {
    return this.obtener<ReporteHorasDemandadasResponse>('horas-demandadas', desde, hasta);
  }

  inasistencias(desde: string, hasta: string): Observable<ReporteInasistenciasResponse> {
    return this.obtener<ReporteInasistenciasResponse>('inasistencias', desde, hasta);
  }

  ingresosPorTratamiento(desde: string, hasta: string): Observable<ReporteIngresosTratamientoResponse> {
    return this.obtener<ReporteIngresosTratamientoResponse>('ingresos-por-tratamiento', desde, hasta);
  }

  private obtener<T>(recurso: string, desde: string, hasta: string): Observable<T> {
    const params = new HttpParams().set('desde', desde).set('hasta', hasta);
    return this.http.get<T>(`${this.reportesUrl}/${recurso}`, { params });
  }
}