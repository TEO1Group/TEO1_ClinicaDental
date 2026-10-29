import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from '../services/auth.service';
import { NotificacionService } from '../notificacion/service/notificacion.service';

describe('authInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let authServiceSpy: { limpiarSesion: ReturnType<typeof vi.fn> };
  let routerSpy: { navigate: ReturnType<typeof vi.fn> };
  let notificacionServiceSpy: { error: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    localStorage.clear();
    authServiceSpy = { limpiarSesion: vi.fn() };
    routerSpy = { navigate: vi.fn() };
    notificacionServiceSpy = { error: vi.fn() };

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: authServiceSpy },
        { provide: Router, useValue: routerSpy },
        { provide: NotificacionService, useValue: notificacionServiceSpy }
      ]
    });

    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('agrega el encabezado Authorization cuando existe token en localStorage', () => {
    localStorage.setItem('token', 'mi-token-jwt');

    http.get('/api/citas').subscribe();

    const req = httpMock.expectOne('/api/citas');
    expect(req.request.headers.get('Authorization')).toBe('Bearer mi-token-jwt');
    req.flush([]);
  });

  it('no agrega el encabezado Authorization si no hay token en localStorage', () => {
    http.get('/api/citas').subscribe();

    const req = httpMock.expectOne('/api/citas');
    expect(req.request.headers.has('Authorization')).toBe(false);
    req.flush([]);
  });

  it('limpia sesion, notifica y redirige a login en error 401 de ruta protegida', () => {
    let capturedStatus = 0;

    http.get('/api/citas').subscribe({
      error: (err: any) => (capturedStatus = err.status)
    });

    const req = httpMock.expectOne('/api/citas');
    req.flush({ mensaje: 'No autorizado' }, { status: 401, statusText: 'Unauthorized' });

    expect(authServiceSpy.limpiarSesion).toHaveBeenCalled();
    expect(notificacionServiceSpy.error).toHaveBeenCalledWith('Tu sesión expiró. Inicia sesión nuevamente.');
    expect(routerSpy.navigate).toHaveBeenCalledWith(['']);
    expect(capturedStatus).toBe(401);
  });

  it('no redirige ni limpia sesion en error 401 de login publico', () => {
    http.post('/api/auth/login', {}).subscribe({
      error: () => {}
    });

    const req = httpMock.expectOne('/api/auth/login');
    req.flush({ mensaje: 'Credenciales incorrectas' }, { status: 401, statusText: 'Unauthorized' });

    expect(authServiceSpy.limpiarSesion).not.toHaveBeenCalled();
    expect(routerSpy.navigate).not.toHaveBeenCalled();
  });

  it('notifica falta de permisos ante un error 403', () => {
    http.get('/api/planes').subscribe({
      error: () => {}
    });

    const req = httpMock.expectOne('/api/planes');
    req.flush({ mensaje: 'Prohibido' }, { status: 403, statusText: 'Forbidden' });

    expect(notificacionServiceSpy.error).toHaveBeenCalledWith('No tienes permisos para acceder a esta sección.');
  });
});
