import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ApiService } from './api.service';
import { LoginRequest } from '../../login/models/login-request.model';
import { LoginResponse } from '../../login/models/login-response.model';
import { PacienteRegistroRequest } from '../../registro/models/paciente-registro.model';
import { RegistroResponse } from '../../registro/models/registro-response.model';

/**
 * Contrato preparado para POST /auth/login.
 * Body: { usuario: string, password: string }
 * Response esperada: { token: string }
 */
@Injectable({ providedIn: 'root' })
export class AuthService extends ApiService {
  constructor(http: HttpClient) {
    super(http);
  }

  login(request: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.baseUrl}/auth/login`, request);
  }

  registrar(request: PacienteRegistroRequest): Observable<RegistroResponse> {
    return this.http.post<RegistroResponse>(`${this.baseUrl}/auth/registro`, request);
  }

  saveToken(token: string): void {
    localStorage.setItem('jwt', token);
  }
}