import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { HttpParams } from '@angular/common/http';
import { ApiService } from '../../core/services/api.service';
import {
  TratamientoRequest,
  TratamientoResponse,
  TratamientosPacienteResponse
} from '../models/tratamiento.model';

@Injectable({ providedIn: 'root' })
export class TratamientoService extends ApiService {
  private readonly tratamientosUrl = `${this.baseUrl}/tratamientos`;

  listar(incluirInactivos = false): Observable<TratamientoResponse[]> {
    const params = new HttpParams().set('incluirInactivos', incluirInactivos);
    return this.http.get<TratamientoResponse[]>(this.tratamientosUrl, { params });
  }

  crear(request: TratamientoRequest): Observable<TratamientoResponse> {
    return this.http.post<TratamientoResponse>(this.tratamientosUrl, request);
  }

  actualizar(id: string, request: TratamientoRequest): Observable<TratamientoResponse> {
    return this.http.put<TratamientoResponse>(`${this.tratamientosUrl}/${id}`, request);
  }

  listarPorPaciente(idCliente: string): Observable<TratamientosPacienteResponse> {
    return this.http.get<TratamientosPacienteResponse>(
      `${this.baseUrl}/pacientes/${idCliente}/tratamientos`
    );
  }
}