import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { ApiService } from '../../core/services/api.service';
import { TratamientosPacienteResponse } from '../models/tratamiento.model';

@Injectable({ providedIn: 'root' })
export class MisTratamientosService extends ApiService {
  private readonly misTratamientosUrl = `${this.baseUrl}/mis-tratamientos`;

  listarMisTratamientos(): Observable<TratamientosPacienteResponse> {
    return this.http.get<TratamientosPacienteResponse>(this.misTratamientosUrl);
  }
}