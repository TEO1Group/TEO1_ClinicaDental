import { HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../core/services/api.service';
import { ReporteCitasPorDoctorResponse } from '../models/reporte-citas.model';

@Injectable({ providedIn: 'root' })
export class ReporteService extends ApiService {
  private readonly reportesUrl = `${this.baseUrl}/reportes`;

  citasPorDoctor(desde: string, hasta: string): Observable<ReporteCitasPorDoctorResponse> {
    const params = new HttpParams()
      .set('desde', desde)
      .set('hasta', hasta);

    return this.http.get<ReporteCitasPorDoctorResponse>(
      `${this.reportesUrl}/citas-por-doctor`,
      { params }
    );
  }
}