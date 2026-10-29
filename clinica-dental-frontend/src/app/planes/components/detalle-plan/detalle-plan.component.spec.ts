import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideZonelessChangeDetection, signal } from '@angular/core';
import { provideRouter } from '@angular/router';
import { ActivatedRoute } from '@angular/router';
import { vi } from 'vitest';
import { DetallePlanComponent } from './detalle-plan.component';
import { AuthService } from '../../../core/services/auth.service';
import { NotificacionService } from '../../../core/notificacion/service/notificacion.service';
import { PlanTratamientoResponse, AbonosPlanResponse } from '../../models/plan-tratamiento.model';

describe('DetallePlanComponent', () => {
  let httpMock: HttpTestingController;
  let notificacionSpy: any;
  let authServiceMock: any;

  const planMock: PlanTratamientoResponse = {
    idPlan: 'plan-1', idCliente: 'cli-1', nombreCliente: 'Ana', apellidoCliente: 'Lopez',
    idDoctor: 'doc-1', nombreDoctor: 'Carlos', apellidoDoctor: 'Perez',
    idTratamiento: 'trat-1', nombreTratamiento: 'Endodoncia', estado: 'ACTIVO',
    fechaCreacion: '2026-10-01T10:00:00', totalSesiones: 6, sesionesAtendidas: 2,
    sesionesAgendadas: 1, sesionesPendientes: 3, porcentajeAvance: 33,
    costoTotal: 1800, totalAbonado: 600, saldo: 1200, citas: [],
  };

  const abonosMock: AbonosPlanResponse = {
    idPlan: 'plan-1', costoTotal: 1800, totalAbonado: 600, saldo: 1200,
    abonos: [
      { idAbono: 'ab-1', idPlan: 'plan-1', monto: 300, fecha: '2026-10-10T10:00:00', idRegistradoPor: 'usr-1' },
      { idAbono: 'ab-2', idPlan: 'plan-1', monto: 300, fecha: '2026-10-15T10:00:00', idRegistradoPor: 'usr-1' },
    ],
  };

  beforeEach(async () => {
    notificacionSpy = { exito: vi.fn(), error: vi.fn() };
    authServiceMock = { rol: signal('ADMIN') };

    await TestBed.configureTestingModule({
      imports: [DetallePlanComponent],
      providers: [
        provideZonelessChangeDetection(),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: AuthService, useValue: authServiceMock },
        { provide: NotificacionService, useValue: notificacionSpy },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { paramMap: { get: () => 'plan-1' } } },
        },
      ],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('debe cargar el plan y los abonos al inicializar', async () => {
    const fixture = TestBed.createComponent(DetallePlanComponent);
    await fixture.whenStable();

    httpMock.expectOne('/api/planes/plan-1').flush(planMock);
    httpMock.expectOne('/api/planes/plan-1/abonos').flush(abonosMock);

    expect(fixture.componentInstance.plan()).toEqual(planMock);
    expect(fixture.componentInstance.abonosData()).toEqual(abonosMock);
  });

  it('ADMIN debe poder registrar abonos', async () => {
    const fixture = TestBed.createComponent(DetallePlanComponent);
    await fixture.whenStable();
    httpMock.expectOne('/api/planes/plan-1').flush(planMock);
    httpMock.expectOne('/api/planes/plan-1/abonos').flush(abonosMock);

    expect(fixture.componentInstance.puedeRegistrarAbono()).toBe(true);
    expect(fixture.componentInstance.puedeVerAbonos()).toBe(true);
    expect(fixture.componentInstance.puedeAgendarSesion()).toBe(true);
  });

  it('DOCTOR debe ver abonos pero no registrar', async () => {
    authServiceMock.rol.set('DOCTOR');
    const fixture = TestBed.createComponent(DetallePlanComponent);
    await fixture.whenStable();
    httpMock.expectOne('/api/planes/plan-1').flush(planMock);
    httpMock.expectOne('/api/planes/plan-1/abonos').flush(abonosMock);

    expect(fixture.componentInstance.puedeRegistrarAbono()).toBe(false);
    expect(fixture.componentInstance.puedeVerAbonos()).toBe(true);
    expect(fixture.componentInstance.puedeAgendarSesion()).toBe(false);
  });

  it('debe mostrar error 409 al registrar abono que supera el saldo', async () => {
    const fixture = TestBed.createComponent(DetallePlanComponent);
    await fixture.whenStable();
    httpMock.expectOne('/api/planes/plan-1').flush(planMock);
    httpMock.expectOne('/api/planes/plan-1/abonos').flush(abonosMock);

    fixture.componentInstance.abrirModalAbono();
    fixture.componentInstance.abonoForm.patchValue({ monto: 5000 });
    fixture.componentInstance.registrarAbono();

    const req = httpMock.expectOne('/api/planes/plan-1/abonos');
    expect(req.request.method).toBe('POST');
    req.flush(
      { mensaje: 'El monto no puede superar el saldo del plan' },
      { status: 409, statusText: 'Conflict' }
    );

    expect(fixture.componentInstance.errorAbono()).toBe('El monto no puede superar el saldo del plan');
  });

  it('debe mostrar error 409 al agendar sesión de paciente bloqueado', async () => {
    const fixture = TestBed.createComponent(DetallePlanComponent);
    await fixture.whenStable();
    httpMock.expectOne('/api/planes/plan-1').flush(planMock);
    httpMock.expectOne('/api/planes/plan-1/abonos').flush(abonosMock);

    fixture.componentInstance.sesionForm.patchValue({
      fecha: '2026-11-01', hora: '10:00',
    });
    fixture.componentInstance.agendarSesion();

    const req = httpMock.expectOne('/api/planes/plan-1/citas');
    expect(req.request.method).toBe('POST');
    req.flush(
      { mensaje: 'El paciente tiene 4 inasistencias y no puede agendar citas' },
      { status: 409, statusText: 'Conflict' }
    );

    expect(notificacionSpy.error).toHaveBeenCalledWith(
      'El paciente tiene 4 inasistencias y no puede agendar citas'
    );
  });
});