import { Injectable } from '@angular/core';
import { Observable, computed, signal, tap } from 'rxjs';
import { jwtDecode } from 'jwt-decode';
import { ApiService } from './api.service';
import { LoginRequest, LoginResponse } from '../../login/models/login.model';
import { PacienteRegistroRequest } from '../../registro/models/paciente-registro.model';
import { RegistroResponse } from '../../registro/models/registro-response.model';

interface DecodedToken {
  rol: string;
  exp: number;
}

@Injectable({ providedIn: 'root' })
export class AuthService extends ApiService {
  private readonly loginUrl = `${this.baseUrl}/auth/login`;
  private readonly registroUrl = `${this.baseUrl}/auth/registro`;
  private readonly _token = signal<string | null>(null);
  private readonly _rol = signal<string | null>(null);

  readonly token = this._token.asReadonly();
  readonly rol = this._rol.asReadonly();
  readonly estaAutenticado = computed(() => this._token() !== null);

  login(request: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(this.loginUrl, request).pipe(
      tap((response) => this.guardarSesion(response))
    );
  }

  registrar(request: PacienteRegistroRequest): Observable<RegistroResponse> {
    return this.http.post<RegistroResponse>(this.registroUrl, request);
  }

  registro(request: PacienteRegistroRequest): Observable<RegistroResponse> {
    return this.registrar(request);
  }

  guardarSesion(response: LoginResponse): void {
    const decoded = jwtDecode<DecodedToken>(response.token);
    this._token.set(response.token);
    this._rol.set(decoded.rol);
    localStorage.setItem('token', response.token);
  }

  saveToken(token: string): void {
    this.guardarSesion({ token });
  }

  limpiarSesion(): void {
    this._token.set(null);
    this._rol.set(null);
    localStorage.removeItem('token');
  }

  obtenerRol(): string | null {
    return this._rol();
  }

  cargarTokenDesdeStorage(): void {
    const token = localStorage.getItem('token');
    if (!token) {
      return;
    }

    try {
      const decoded = jwtDecode<DecodedToken>(token);
      if (decoded.exp * 1000 > Date.now()) {
        this._token.set(token);
        this._rol.set(decoded.rol);
      } else {
        this.limpiarSesion();
      }
    } catch {
      this.limpiarSesion();
    }
  }
}
