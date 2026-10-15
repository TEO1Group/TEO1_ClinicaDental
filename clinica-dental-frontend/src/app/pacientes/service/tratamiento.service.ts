import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../core/services/api.service';
import {
  TratamientoResponse,
  TratamientosPacienteResponse
} from '../models/tratamiento.model';

@Injectable({ providedIn: 'root' })
export class TratamientoService extends ApiService {
  private readonly tratamientosUrl = `${this.baseUrl}/tratamientos`;

  listarCatalogo(): Observable<TratamientoResponse[]> {
    return this.http.get<TratamientoResponse[]>(this.tratamientosUrl);
  }

  listarPorPaciente(idCliente: string): Observable<TratamientosPacienteResponse> {
    return this.http.get<TratamientosPacienteResponse>(
      `${this.baseUrl}/pacientes/${idCliente}/tratamientos`
    );
  }
}