import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { PlanTratamientoService } from './plan-tratamiento.service';
import {
  AbonoPlanRequest,
  AbonoPlanResponse,
  PlanTratamientoRequest,
  PlanTratamientoResponse,
  SesionPlanRequest
} from '../models/plan-tratamiento.model';

describe('PlanTratamientoService', () => {
  let service: PlanTratamientoService;
  let httpMock: HttpTestingController;

  const mockPlan: PlanTratamientoResponse = {
    idPlan: 'plan-1',
    idCliente: 'cliente-1',
    nombreCliente: 'Ana',
    apellidoCliente: 'Lopez',
    idDoctor: 'doctor-1',
    nombreDoctor: 'Lucia',
    apellidoDoctor: 'Morales',
    idTratamiento: 'tratamiento-1',
    nombreTratamiento: 'Ortodoncia',
    estado: 'ACTIVO',
    fechaCreacion: '2026-10-20',
    totalSesiones: 5,
    sesionesAtendidas: 1,
    sesionesAgendadas: 1,
    sesionesPendientes: 3,
    porcentajeAvance: 20,
    costoTotal: 2500,
    totalAbonado: 500,
    saldo: 2000
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        PlanTratamientoService
      ]
    });

    service = TestBed.inject(PlanTratamientoService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('lista planes de tratamiento mediante GET', () => {
    let planes: PlanTratamientoResponse[] = [];
    service.listarPlanes({ estado: 'ACTIVO' }).subscribe(res => (planes = res));

    const req = httpMock.expectOne(r => r.url === '/api/planes');
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('estado')).toBe('ACTIVO');
    req.flush([mockPlan]);

    expect(planes.length).toBe(1);
    expect(planes[0].idPlan).toBe('plan-1');
  });

  it('crea un plan de tratamiento mediante POST', () => {
    const request: PlanTratamientoRequest = {
      idCliente: 'cliente-1',
      idDoctor: 'doctor-1',
      idTratamiento: 'tratamiento-1',
      totalSesiones: 5,
      costoTotal: 2500
    };

    let creado: PlanTratamientoResponse | undefined;
    service.crearPlan(request).subscribe(res => (creado = res));

    const req = httpMock.expectOne('/api/planes');
    expect(req.request.method).toBe('POST');
    req.flush(mockPlan);

    expect(creado).toEqual(mockPlan);
  });

  it('registra un abono al plan mediante POST', () => {
    const request: AbonoPlanRequest = {
      monto: 500
    };

    const mockAbono: AbonoPlanResponse = {
      idAbono: 'abono-1',
      idPlan: 'plan-1',
      monto: 500,
      fecha: '2026-10-25T10:00:00',
      idRegistradoPor: 'admin-1'
    };

    let abonoRegistrado: AbonoPlanResponse | undefined;
    service.registrarAbono('plan-1', request).subscribe(res => (abonoRegistrado = res));

    const req = httpMock.expectOne('/api/planes/plan-1/abonos');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(request);
    req.flush(mockAbono);

    expect(abonoRegistrado?.idAbono).toBe('abono-1');
  });

  it('maneja error cuando falla el registro de abono con monto excesivo (400)', () => {
    const request: AbonoPlanRequest = {
      monto: 99999
    };

    let status = 0;
    service.registrarAbono('plan-1', request).subscribe({
      error: err => (status = err.status)
    });

    const req = httpMock.expectOne('/api/planes/plan-1/abonos');
    req.flush({ mensaje: 'El abono supera el saldo pendiente' }, { status: 400, statusText: 'Bad Request' });

    expect(status).toBe(400);
  });

  it('agenda una sesion del plan mediante POST a /planes/:id/citas', () => {
    const request: SesionPlanRequest = {
      fecha: '2026-10-28',
      hora: '11:00',
      notas: 'Segunda sesion'
    };

    let exito = false;
    service.agendarSesion('plan-1', request).subscribe(() => (exito = true));

    const req = httpMock.expectOne('/api/planes/plan-1/citas');
    expect(req.request.method).toBe('POST');
    req.flush({ idCita: 'cita-sesion-2', estado: 'AGENDADA' });

    expect(exito).toBe(true);
  });
});
