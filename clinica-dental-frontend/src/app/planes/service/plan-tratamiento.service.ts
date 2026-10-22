import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { HttpParams } from '@angular/common/http';
import { ApiService } from '../../core/services/api.service';
import { CitaResponse } from '../../citas/models/cita.model';
import {
  AbonoPlanRequest,
  AbonoPlanResponse,
  AbonosPlanResponse,
  EstadoPlan,
  PlanTratamientoRequest,
  PlanTratamientoResponse,
  SesionPlanRequest
} from '../models/plan-tratamiento.model';

export interface PlanFiltros {
  idCliente?: string;
  idDoctor?: string;
  estado?: EstadoPlan;
}

@Injectable({ providedIn: 'root' })
export class PlanTratamientoService extends ApiService {
  private readonly planesUrl = `${this.baseUrl}/planes`;

  listarPlanes(filtros?: PlanFiltros): Observable<PlanTratamientoResponse[]> {
    let params = new HttpParams();
    if (filtros?.idCliente) params = params.set('idCliente', filtros.idCliente);
    if (filtros?.idDoctor) params = params.set('idDoctor', filtros.idDoctor);
    if (filtros?.estado) params = params.set('estado', filtros.estado);
    return this.http.get<PlanTratamientoResponse[]>(this.planesUrl, { params });
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

  listarMisPlanes(): Observable<PlanTratamientoResponse[]> {
    return this.http.get<PlanTratamientoResponse[]>(`${this.baseUrl}/mis-planes`);
  }

  listarAbonos(idPlan: string): Observable<AbonosPlanResponse> {
    return this.http.get<AbonosPlanResponse>(`${this.planesUrl}/${idPlan}/abonos`);
  }

  registrarAbono(idPlan: string, request: AbonoPlanRequest): Observable<AbonoPlanResponse> {
    return this.http.post<AbonoPlanResponse>(`${this.planesUrl}/${idPlan}/abonos`, request);
  }
}