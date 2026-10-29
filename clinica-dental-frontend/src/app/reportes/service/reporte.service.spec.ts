import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ReporteService } from './reporte.service';
import {
  ReporteCitasPorDoctorResponse,
  ReporteGananciasResponse,
  ReporteInasistenciasResponse
} from '../models/reporte-citas.model';

describe('ReporteService', () => {
  let service: ReporteService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        ReporteService
      ]
    });

    service = TestBed.inject(ReporteService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('obtiene reporte de citas por doctor mediante GET con fechas', () => {
    const mockResponse: ReporteCitasPorDoctorResponse = {
      desde: '2026-10-01',
      hasta: '2026-10-31',
      doctores: [
        {
          idDoctor: 'doc-1',
          nombreDoctor: 'Carlos',
          apellidoDoctor: 'Perez',
          especialidad: 'General',
          agendadas: 2,
          atendidas: 5,
          canceladas: 1,
          noAsistio: 0,
          total: 8
        }
      ]
    };

    let respuesta: ReporteCitasPorDoctorResponse | undefined;
    service.citasPorDoctor('2026-10-01', '2026-10-31').subscribe(res => (respuesta = res));

    const req = httpMock.expectOne(r => r.url === '/api/reportes/citas-por-doctor');
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('desde')).toBe('2026-10-01');
    expect(req.request.params.get('hasta')).toBe('2026-10-31');
    req.flush(mockResponse);

    expect(respuesta).toEqual(mockResponse);
  });

  it('obtiene reporte de ganancias con tratamientos y abonos separados', () => {
    const mockGanancias: ReporteGananciasResponse = {
      desde: '2026-10-01',
      hasta: '2026-10-29',
      totalTratamientos: 15000,
      totalAbonos: 4000,
      doctores: [
        {
          idDoctor: 'doc-1',
          nombreDoctor: 'Lucia',
          apellidoDoctor: 'Morales',
          cantidadTratamientos: 10,
          total: 15000
        }
      ]
    };

    let respuesta: ReporteGananciasResponse | undefined;
    service.ganancias('2026-10-01', '2026-10-29').subscribe(res => (respuesta = res));

    const req = httpMock.expectOne(r => r.url === '/api/reportes/ganancias');
    expect(req.request.method).toBe('GET');
    req.flush(mockGanancias);

    expect(respuesta?.totalTratamientos).toBe(15000);
    expect(respuesta?.totalAbonos).toBe(4000);
  });

  it('maneja error cuando el backend rechaza rango de fechas invalido (400)', () => {
    let status = 0;
    let mensaje = '';

    service.inasistencias('2026-10-30', '2026-10-01').subscribe({
      error: err => {
        status = err.status;
        mensaje = err.error.mensaje;
      }
    });

    const req = httpMock.expectOne(r => r.url === '/api/reportes/inasistencias');
    req.flush(
      { mensaje: 'La fecha desde no puede ser posterior a hasta' },
      { status: 400, statusText: 'Bad Request' }
    );

    expect(status).toBe(400);
    expect(mensaje).toBe('La fecha desde no puede ser posterior a hasta');
  });
});
