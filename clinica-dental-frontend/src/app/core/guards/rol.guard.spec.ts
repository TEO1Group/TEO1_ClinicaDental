import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { rolGuard } from './rol.guard';
import { AuthService } from '../services/auth.service';
import { NotificacionService } from '../notificacion/service/notificacion.service';

describe('rolGuard', () => {
  let authServiceSpy: { obtenerRol: ReturnType<typeof vi.fn> };
  let routerSpy: { navigate: ReturnType<typeof vi.fn> };
  let notificacionServiceSpy: { error: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    authServiceSpy = { obtenerRol: vi.fn() };
    routerSpy = { navigate: vi.fn() };
    notificacionServiceSpy = { error: vi.fn() };

    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: authServiceSpy },
        { provide: Router, useValue: routerSpy },
        { provide: NotificacionService, useValue: notificacionServiceSpy }
      ]
    });
  });

  it('permite el acceso cuando el usuario tiene un rol permitido', () => {
    authServiceSpy.obtenerRol.mockReturnValue('ADMIN');

    const resultado = TestBed.runInInjectionContext(() =>
      rolGuard(['ADMIN', 'SECRETARIA'])({} as any, {} as any)
    );

    expect(resultado).toBe(true);
    expect(routerSpy.navigate).not.toHaveBeenCalled();
  });

  it('rechaza el acceso y redirige a dashboard cuando el usuario tiene un rol no permitido', () => {
    authServiceSpy.obtenerRol.mockReturnValue('CLIENTE');

    const resultado = TestBed.runInInjectionContext(() =>
      rolGuard(['ADMIN', 'SECRETARIA'])({} as any, {} as any)
    );

    expect(resultado).toBe(false);
    expect(notificacionServiceSpy.error).toHaveBeenCalledWith('No tienes permisos para acceder a esta sección.');
    expect(routerSpy.navigate).toHaveBeenCalledWith(['/dashboard']);
  });

  it('rechaza el acceso y redirige a login cuando no hay sesion iniciada', () => {
    authServiceSpy.obtenerRol.mockReturnValue(null);

    const resultado = TestBed.runInInjectionContext(() =>
      rolGuard(['ADMIN'])({} as any, {} as any)
    );

    expect(resultado).toBe(false);
    expect(routerSpy.navigate).toHaveBeenCalledWith(['']);
  });
});

