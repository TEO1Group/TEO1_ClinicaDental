import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { VersionResponse, VersionService } from './version.service';

describe('VersionService', () => {
  let service: VersionService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        VersionService
      ]
    });

    service = TestBed.inject(VersionService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('obtiene la version desplegada correctamente mediante GET', () => {
    const mockVersion: VersionResponse = {
      commit: 'dc45780',
      fechaConstruccion: '2026-10-28T16:22:12Z'
    };

    let respuesta: VersionResponse | null | undefined;
    service.obtenerVersion().subscribe(res => (respuesta = res));

    const req = httpMock.expectOne('/api/version');
    expect(req.request.method).toBe('GET');
    req.flush(mockVersion);

    expect(respuesta?.commit).toBe('dc45780');
  });

  it('devuelve null y no rompe la aplicacion ante un fallo del endpoint de version', () => {
    let respuesta: VersionResponse | null | undefined;
    service.obtenerVersion().subscribe(res => (respuesta = res));

    const req = httpMock.expectOne('/api/version');
    req.flush({ mensaje: 'Error interno' }, { status: 500, statusText: 'Server Error' });

    expect(respuesta).toBeNull();
  });
});
