import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../core/services/api.service';
import { CitaResponse } from '../../citas/models/cita.model';
import {
  PlanTratamientoRequest,
  PlanTratamientoResponse,
  SesionPlanRequest
} from '../models/plan-tratamiento.model';

@Injectable({ providedIn: 'root' })
export class PlanTratamientoService extends ApiService {
  private readonly planesUrl = `${this.baseUrl}/planes`;

  listarPlanes(): Observable<PlanTratamientoResponse[]> {
    return this.http.get<PlanTratamientoResponse[]>(this.planesUrl);
  }

  obtenerPlan(id: string): Observable<PlanTratamientoResponse> {
    return this.http.get<PlanTratamientoResponse>(`${this.planesUrl}/${id}`);
  }

  crearPlan(request: PlanTratamientoRequest): Observable<PlanTratamientoResponse> {
    return this.http.post<PlanTratamientoResponse>(this.planesUrl, request);
  }

  agendarSesion(id: string, request: SesionPlanRequest): Observable<CitaResponse> {
    return this.http.post<CitaResponse>(`${this.planesUrl}/${id}/citas`, request);
  }
}