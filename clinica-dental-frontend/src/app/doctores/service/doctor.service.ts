import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../core/services/api.service';
import { DoctorResponse, DoctorUpdateRequest } from '../models/doctor.model';
import { HorarioRequest, HorarioResponse } from '../models/horario.mode';

@Injectable({ providedIn: 'root' })
export class DoctorService extends ApiService {
  private readonly doctoresUrl = `${this.baseUrl}/doctores`;

  listarDoctores(): Observable<DoctorResponse[]> {
    return this.http.get<DoctorResponse[]>(this.doctoresUrl);
  }

  obtenerDoctor(id: string): Observable<DoctorResponse> {
    return this.http.get<DoctorResponse>(`${this.doctoresUrl}/${id}`);
  }

  actualizarDoctor(id: string, request: DoctorUpdateRequest): Observable<DoctorResponse> {
    return this.http.put<DoctorResponse>(`${this.doctoresUrl}/${id}`, request);
  }

  desactivarDoctor(id: string): Observable<void> {
    return this.http.delete<void>(`${this.doctoresUrl}/${id}`);
  }

  listarHorarios(id: string): Observable<HorarioResponse[]> {
    return this.http.get<HorarioResponse[]>(`${this.doctoresUrl}/${id}/horarios`);
  }

  agregarHorario(id: string, request: HorarioRequest): Observable<HorarioResponse> {
    return this.http.post<HorarioResponse>(`${this.doctoresUrl}/${id}/horarios`, request);
  }

  actualizarHorario(doctorId: string, horarioId: string, request: HorarioRequest): Observable<HorarioResponse> {
    return this.http.put<HorarioResponse>(`${this.doctoresUrl}/${doctorId}/horarios/${horarioId}`, request);
  }

  eliminarHorario(doctorId: string, horarioId: string): Observable<void> {
    return this.http.delete<void>(`${this.doctoresUrl}/${doctorId}/horarios/${horarioId}`);
  }
}