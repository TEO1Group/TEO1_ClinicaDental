import { Injectable, signal, computed, inject } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { ApiService } from './api.service';
import { jwtDecode } from 'jwt-decode';
import { PacienteRegistroRequest, RegistroResponse } from '../../registro/models/paciente-registro.model';
import { LoginRequest, LoginResponse } from '../../login/models/login.model';
import { UsuarioActualResponse } from '../models/usuario-actual.model';
import { NotificacionService } from '../notificacion/service/notificacion.service';
import { CitaService } from '../../citas/service/cita.service';

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
  private readonly meUrl = `${this.baseUrl}/auth/me`;

  private readonly notificacionService = inject(NotificacionService);
  private readonly citaService = inject(CitaService);

  private readonly _token = signal<string | null>(null);
  private readonly _rol = signal<string | null>(null);
  private readonly _usuarioActual = signal<UsuarioActualResponse | null>(null);
  private notificacionProximasMostrada = false;

  readonly token = this._token.asReadonly();
  readonly rol = this._rol.asReadonly();
  readonly usuarioActual = this._usuarioActual.asReadonly();
  readonly estaAutenticado = computed(() => this._token() !== null);

  login(request: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(this.loginUrl, request).pipe(
      tap((response) => {
        this.guardarSesion(response);
        this.cargarUsuarioActual();
      })
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

  cargarUsuarioActual(): void {
    this.http.get<UsuarioActualResponse>(this.meUrl).subscribe({
      next: (usuario) => {
        this._usuarioActual.set(usuario);
        this.cargarCitasProximas();
      },
      error: () => this._usuarioActual.set(null)
    });
  }

  private cargarCitasProximas(): void {
    this.citaService.cargarProximas().subscribe({
      next: (citas) => {
        if (citas.length > 0 && !this.notificacionProximasMostrada) {
          this.notificacionProximasMostrada = true;
          this.notificacionService.info(
            `Tienes ${citas.length} cita${citas.length === 1 ? '' : 's'} en las próximas 48 horas.`
          );
        }
      },
      error: () => {
        // Silencioso: si falla, la campana queda sin citas
      }
    });
  }

  limpiarSesion(): void {
    this._token.set(null);
    this._rol.set(null);
    this._usuarioActual.set(null);
    this.notificacionProximasMostrada = false;
    this.citaService.limpiarProximas();
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
          this.cargarUsuarioActual();
        } else {
          this.limpiarSesion();
        }
      } catch {
        this.limpiarSesion();
      }
    }
  }
}