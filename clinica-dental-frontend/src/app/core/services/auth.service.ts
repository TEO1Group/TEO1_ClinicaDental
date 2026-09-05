import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment.development';

export interface LoginRequest {
  usuario: string;
  password: string;
}

export interface LoginResponse {
  token: string;
}

export interface RegistroRequest {
  dpi: string;
  nombre: string;
  celular: string;
  password: string;
}

export interface RegistroResponse {
  mensaje: string;
  id: number;
}

/**
 * Contrato preparado para POST /auth/login.
 * Body: { usuario: string, password: string }
 * Response esperada: { token: string }
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly loginUrl = `${environment.apiUrl}/auth/login`;

  login(request: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(this.loginUrl, request);
  }

  registrar(request: RegistroRequest): Observable<RegistroResponse> {
    return this.http.post<RegistroResponse>(`${environment.apiUrl}/auth/registro`, request);
  }

  saveToken(token: string): void {
    localStorage.setItem('jwt', token);
  }
}