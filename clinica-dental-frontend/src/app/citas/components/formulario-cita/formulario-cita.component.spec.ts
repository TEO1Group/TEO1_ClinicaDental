import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideZonelessChangeDetection, signal } from '@angular/core';
import { provideRouter } from '@angular/router';
import { vi } from 'vitest';
import { FormularioCitaComponent } from './formulario-cita.component';
import { AuthService } from '../../../core/services/auth.service';
import { NotificacionService } from '../../../core/notificacion/service/notificacion.service';
import { DoctorResponse } from '../../../doctores/models/doctor.model';
import { ClienteResponse } from '../../../pacientes/models/paciente.model';

describe('FormularioCitaComponent', () => {
  let httpMock: HttpTestingController;
  let notificacionSpy: any;

  const authServiceMock = {
    rol: signal('ADMIN'),
  };

  const doctoresMock: DoctorResponse[] = [
    {
      idDoctor: 'doc-1', idUsuario: 'usr-1', nombre: 'Carlos', apellido: 'Perez',
      email: 'carlos@test.com', telefono: '12345678', especialidad: 'Ortodoncia',
      numeroColegiado: '123', numeroClinica: '01', calificacionPromedio: 4.5,
    },
  ];

  const pacientesMock: ClienteResponse[] = [
    {
      idCliente: 'cli-1', idUsuario: 'usr-2', nombre: 'Ana', apellido: 'Lopez',
      email: 'ana@test.com', telefono: '87654321', dpi: '1234567890123',
      direccion: 'Calle 1', fechaNacimiento: '1990-01-01',
      enListaNegra: false, motivoListaNegra: null, estado: 'ACTIVO',
    },
  ];

  beforeEach(async () => {
    notificacionSpy = { exito: vi.fn(), error: vi.fn() };

    await TestBed.configureTestingModule({
      imports: [FormularioCitaComponent],
      providers: [
        provideZonelessChangeDetection(),
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: AuthService, useValue: authServiceMock },
        { provide: NotificacionService, useValue: notificacionSpy },
      ],
    }).compileComponents();

    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('debe cargar doctores y pacientes al inicializar', async () => {
    const fixture = TestBed.createComponent(FormularioCitaComponent);
    await fixture.whenStable();

    const reqDoctores = httpMock.expectOne('/api/doctores');
    expect(reqDoctores.request.method).toBe('GET');
    reqDoctores.flush(doctoresMock);

    const reqPacientes = httpMock.expectOne('/api/pacientes');
    expect(reqPacientes.request.method).toBe('GET');
    reqPacientes.flush(pacientesMock);

    expect(fixture.componentInstance.doctores()).toEqual(doctoresMock);
    expect(fixture.componentInstance.pacientes()).toEqual(pacientesMock);
  });

  it('debe mostrar error 409 de inasistencias al agendar', async () => {
    const fixture = TestBed.createComponent(FormularioCitaComponent);
    await fixture.whenStable();

    httpMock.expectOne('/api/doctores').flush(doctoresMock);
    httpMock.expectOne('/api/pacientes').flush(pacientesMock);

    fixture.componentInstance.formulario.patchValue({
      idDoctor: 'doc-1', idCliente: 'cli-1', fecha: '2026-11-01', hora: '10:00',
    });

    httpMock.expectOne('/api/doctores/doc-1/horarios').flush([]);
    httpMock.expectOne('/api/doctores/doc-1/bloqueos-vigentes').flush([]);

    fixture.componentInstance.agendar();

    const req = httpMock.expectOne('/api/citas');
    expect(req.request.method).toBe('POST');
    req.flush(
      { mensaje: 'El paciente tiene 4 inasistencias y no puede agendar citas' },
      { status: 409, statusText: 'Conflict' }
    );

    expect(notificacionSpy.error).toHaveBeenCalledWith(
      'El paciente tiene 4 inasistencias y no puede agendar citas'
    );
  });

  it('debe deshabilitar el botón si el formulario es inválido', async () => {
    const fixture = TestBed.createComponent(FormularioCitaComponent);
    await fixture.whenStable();

    httpMock.expectOne('/api/doctores').flush(doctoresMock);
    httpMock.expectOne('/api/pacientes').flush(pacientesMock);

    expect(fixture.componentInstance.formulario.invalid).toBe(true);
  });
});
