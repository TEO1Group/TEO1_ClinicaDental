import { Injectable, signal, computed } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { ApiService } from './api.service';
import { jwtDecode } from 'jwt-decode';
import { PacienteRegistroRequest, RegistroResponse} from '../../registro/models/paciente-registro.model';
import { LoginRequest, LoginResponse } from '../../login/models/login.model';

interface DecodedToken {
  sub: string;
  rol: string;
  exp: number;
  iat: number;
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

  registro(request: PacienteRegistroRequest): Observable<RegistroResponse> {
    return this.http.post<RegistroResponse>(this.registroUrl, request);
  }

  guardarSesion(response: LoginResponse): void {
    this._token.set(response.token);
    const decoded = jwtDecode<DecodedToken>(response.token);
    this._rol.set(decoded.rol);
    localStorage.setItem('token', response.token);
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
    if (token) {
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
}