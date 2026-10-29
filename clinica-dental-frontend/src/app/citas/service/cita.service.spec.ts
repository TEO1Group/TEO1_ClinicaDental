import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { CitaService } from './cita.service';
import { CitaRequest, CitaResponse, ReprogramarCitaRequest } from '../models/cita.model';

describe('CitaService', () => {
  let service: CitaService;
  let httpMock: HttpTestingController;

  const mockCita: CitaResponse = {
    idCita: 'cita-1',
    idCliente: 'cliente-1',
    idDoctor: 'doctor-1',
    fecha: '2026-10-25',
    hora: '09:00:00',
    estado: 'AGENDADA',
    notas: null,
    motivo: null,
    nombreCliente: 'Ana',
    apellidoCliente: 'Lopez',
    nombreDoctor: 'Lucia',
    apellidoDoctor: 'Morales'
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        CitaService
      ]
    });

    service = TestBed.inject(CitaService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('crea una cita exitosamente mediante POST', () => {
    const request: CitaRequest = {
      idDoctor: 'doctor-1',
      fecha: '2026-10-25',
      hora: '09:00'
    };

    let respuesta: CitaResponse | undefined;
    service.crear(request).subscribe(res => (respuesta = res));

    const req = httpMock.expectOne('/api/citas');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(mockCita);

    expect(respuesta).toEqual(mockCita);
  });

  it('propaga error cuando falla la creacion de cita por solapamiento (409)', () => {
    const request: CitaRequest = {
      idDoctor: 'doctor-1',
      fecha: '2026-10-25',
      hora: '09:00'
    };

    let statusCapturado = 0;
    service.crear(request).subscribe({
      error: err => (statusCapturado = err.status)
    });

    const req = httpMock.expectOne('/api/citas');
    req.flush({ mensaje: 'El doctor ya tiene cita en este horario' }, { status: 409, statusText: 'Conflict' });

    expect(statusCapturado).toBe(409);
  });

  it('lista citas con filtros aplicados mediante GET', () => {
    let citas: CitaResponse[] = [];
    service.listar({ estado: 'AGENDADA', idDoctor: 'doctor-1' }).subscribe(res => (citas = res));

    const req = httpMock.expectOne(r => r.url === '/api/citas');
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('estado')).toBe('AGENDADA');
    expect(req.request.params.get('idDoctor')).toBe('doctor-1');
    req.flush([mockCita]);

    expect(citas.length).toBe(1);
    expect(citas[0].idCita).toBe('cita-1');
  });

  it('reprograma una cita mediante PATCH y maneja error si falla', () => {
    const request: ReprogramarCitaRequest = {
      fecha: '2026-10-26',
      hora: '10:00',
      motivo: 'Cambio de horario solicitado'
    };

    let errorMensaje = '';
    service.reprogramar('cita-1', request).subscribe({
      error: err => (errorMensaje = err.error.mensaje)
    });

    const req = httpMock.expectOne('/api/citas/cita-1/reprogramar');
    expect(req.request.method).toBe('PATCH');
    req.flush({ mensaje: 'Plazo menor a 24 horas' }, { status: 400, statusText: 'Bad Request' });

    expect(errorMensaje).toBe('Plazo menor a 24 horas');
  });
});
