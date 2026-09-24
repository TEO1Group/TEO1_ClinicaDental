import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../../core/services/api.service';
import { PacienteRegistroRequest } from '../../../registro/models/paciente-registro.model';
import { ClienteResponse, ClienteUpdateRequest, ListaNegraRequest } from '../cliente.model';
import { HistorialClinicoRequest, HistorialClinicoResponse } from '../historial.model';

@Injectable({ providedIn: 'root' })
export class PacienteService extends ApiService {
  private readonly pacientesUrl = `${this.baseUrl}/pacientes`;

  listarPacientes(): Observable<ClienteResponse[]> {
    return this.http.get<ClienteResponse[]>(this.pacientesUrl);
  }

  obtenerPaciente(id: string): Observable<ClienteResponse> {
    return this.http.get<ClienteResponse>(`${this.pacientesUrl}/${id}`);
  }

  crearPaciente(request: PacienteRegistroRequest): Observable<ClienteResponse> {
    return this.http.post<ClienteResponse>(this.pacientesUrl, request);
  }

  actualizarPaciente(id: string, request: ClienteUpdateRequest): Observable<ClienteResponse> {
    return this.http.put<ClienteResponse>(`${this.pacientesUrl}/${id}`, request);
  }

  desactivarPaciente(id: string): Observable<void> {
    return this.http.delete<void>(`${this.pacientesUrl}/${id}`);
  }

  actualizarListaNegra(id: string, request: ListaNegraRequest): Observable<ClienteResponse> {
    return this.http.patch<ClienteResponse>(`${this.pacientesUrl}/${id}/lista-negra`, request);
  }

  listarHistorial(id: string): Observable<HistorialClinicoResponse[]> {
    return this.http.get<HistorialClinicoResponse[]>(`${this.pacientesUrl}/${id}/historial`);
  }

  agregarHistorial(id: string, request: HistorialClinicoRequest): Observable<HistorialClinicoResponse> {
    return this.http.post<HistorialClinicoResponse>(`${this.pacientesUrl}/${id}/historial`, request);
  }
}