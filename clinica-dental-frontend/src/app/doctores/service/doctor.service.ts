import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../core/services/api.service';
import { DoctorResponse, DoctorUpdateRequest } from '../models/doctor.model';
import { HorarioRequest, HorarioResponse } from '../models/horario.mode';
import { BloqueoDoctorRequest, BloqueoDoctorResponse } from '../models/bloqueo.model';

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


  listarBloqueos(idDoctor: string): Observable<BloqueoDoctorResponse[]> {
    return this.http.get<BloqueoDoctorResponse[]>(`${this.doctoresUrl}/${idDoctor}/bloqueos`);
  }

  crearBloqueo(idDoctor: string, request: BloqueoDoctorRequest): Observable<BloqueoDoctorResponse> {
    return this.http.post<BloqueoDoctorResponse>(`${this.doctoresUrl}/${idDoctor}/bloqueos`, request);
  }

  eliminarBloqueo(idDoctor: string, idBloqueo: string): Observable<void> {
    return this.http.delete<void>(`${this.doctoresUrl}/${idDoctor}/bloqueos/${idBloqueo}`);
  }
}