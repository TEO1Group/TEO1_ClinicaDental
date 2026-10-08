import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../core/services/api.service';
import { CitaResponse, EstadoCitaRequest } from '../models/cita.model';

@Injectable({ providedIn: 'root' })
export class CitaService extends ApiService {
  private readonly citasUrl = `${this.baseUrl}/citas`;

  listarCitas(): Observable<CitaResponse[]> {
    return this.http.get<CitaResponse[]>(this.citasUrl);
  }

  cambiarEstado(id: string, request: EstadoCitaRequest): Observable<CitaResponse> {
    return this.http.patch<CitaResponse>(`${this.citasUrl}/${id}/estado`, request);
  }
}