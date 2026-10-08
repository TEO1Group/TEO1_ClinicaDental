import { Injectable, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { HttpParams } from '@angular/common/http';
import { ApiService } from '../../core/services/api.service';
import { CitaRequest, CitaResponse, CambioEstadoCitaRequest, CitaFiltros } from '../models/cita.model';

@Injectable({ providedIn: 'root' })
export class CitaService extends ApiService {
    private readonly citasUrl = `${this.baseUrl}/citas`;

    private readonly _proximas = signal<CitaResponse[]>([]);
    readonly proximasSignal = this._proximas.asReadonly();

    

    crear(request: CitaRequest): Observable<CitaResponse> {
        return this.http.post<CitaResponse>(this.citasUrl, request);
    }

    listar(filtros?: CitaFiltros): Observable<CitaResponse[]> {
        let params = new HttpParams();

        if (filtros?.estado) {
            params = params.set('estado', filtros.estado);
        }
        if (filtros?.fecha) {
            params = params.set('fecha', filtros.fecha);
        }
        if (filtros?.idDoctor) {
            params = params.set('idDoctor', filtros.idDoctor);
        }
        if (filtros?.idCliente) {
            params = params.set('idCliente', filtros.idCliente);
        }

        return this.http.get<CitaResponse[]>(this.citasUrl, { params });
    }

    obtener(id: string): Observable<CitaResponse> {
        return this.http.get<CitaResponse>(`${this.citasUrl}/${id}`);
    }

    cambiarEstado(id: string, request: CambioEstadoCitaRequest): Observable<CitaResponse> {
        return this.http.patch<CitaResponse>(`${this.citasUrl}/${id}/estado`, request);
    }

    cargarProximas(): Observable<CitaResponse[]> {
        return this.http.get<CitaResponse[]>(`${this.citasUrl}/proximas`).pipe(
            tap((citas) => this._proximas.set(citas))
        );
    }

    limpiarProximas(): void {
        this._proximas.set([]);
    }

    cancelar(id: string): Observable<CitaResponse> {
        return this.cambiarEstado(id, { estado: 'CANCELADA' });
    }

    completar(id: string): Observable<CitaResponse> {
        return this.cambiarEstado(id, { estado: 'COMPLETADA' });
    }
}