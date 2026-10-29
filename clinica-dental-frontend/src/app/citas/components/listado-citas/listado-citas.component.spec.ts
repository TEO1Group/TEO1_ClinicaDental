import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideZonelessChangeDetection, signal } from '@angular/core';
import { provideRouter } from '@angular/router';
import { vi } from 'vitest';
import { ListadoCitasComponent } from './listado-citas.component';
import { AuthService } from '../../../core/services/auth.service';
import { NotificacionService } from '../../../core/notificacion/service/notificacion.service';
import { CitaResponse } from '../../models/cita.model';

describe('ListadoCitasComponent', () => {
  let httpMock: HttpTestingController;
  let notificacionSpy: any;
  let authServiceMock: any;

  const citasMock: CitaResponse[] = [
    {
      idCita: 'cita-1', idCliente: 'cli-1', idDoctor: 'doc-1',
      fecha: '2026-11-01', hora: '10:00:00', estado: 'AGENDADA',
      notas: null, nombreCliente: 'Ana', apellidoCliente: 'Lopez',
      nombreDoctor: 'Carlos', apellidoDoctor: 'Perez', motivo: null,
    },
    {
      idCita: 'cita-2', idCliente: 'cli-1', idDoctor: 'doc-1',
      fecha: '2026-11-02', hora: '11:00:00', estado: 'CANCELADA',
      notas: null, nombreCliente: 'Ana', apellidoCliente: 'Lopez',
      nombreDoctor: 'Carlos', apellidoDoctor: 'Perez', motivo: 'Reprogramación',
    },
  ];

  beforeEach(async () => {
    notificacionSpy = { exito: vi.fn(), error: vi.fn() };
    authServiceMock = {
      rol: signal('ADMIN'),
      usuarioActual: signal({ idPerfil: 'admin-1' }),
    };

    await TestBed.configureTestingModule({
      imports: [ListadoCitasComponent],
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

  it('debe cargar las citas al inicializar', async () => {
    const fixture = TestBed.createComponent(ListadoCitasComponent);
    await fixture.whenStable();

    const req = httpMock.expectOne('/api/citas');
    expect(req.request.method).toBe('GET');
    req.flush(citasMock);

    expect(fixture.componentInstance.citas()).toEqual(citasMock);
  });

  it('debe filtrar citas por estado', async () => {
    const fixture = TestBed.createComponent(ListadoCitasComponent);
    await fixture.whenStable();

    httpMock.expectOne('/api/citas').flush(citasMock);

    fixture.componentInstance.filtroForm.patchValue({ estado: 'AGENDADA' });
    await fixture.whenStable();

    const filtradas = fixture.componentInstance.citasFiltradas();
    expect(filtradas.length).toBe(1);
    expect(filtradas[0].estado).toBe('AGENDADA');
  });

  it('ADMIN debe poder cancelar, marcar atendida y no asistió', async () => {
    const fixture = TestBed.createComponent(ListadoCitasComponent);
    await fixture.whenStable();
    httpMock.expectOne('/api/citas').flush(citasMock);

    const citaAgendada = citasMock[0];
    expect(fixture.componentInstance.puedeCancelar(citaAgendada)).toBe(true);
    expect(fixture.componentInstance.puedeMarcarAtendida(citaAgendada)).toBe(true);
    expect(fixture.componentInstance.puedeMarcarNoAsistio(citaAgendada)).toBe(true);
    expect(fixture.componentInstance.puedeReprogramar(citaAgendada)).toBe(true);
  });

  it('DOCTOR no debe poder cancelar pero sí marcar atendida', async () => {
    authServiceMock.rol.set('DOCTOR');
    const fixture = TestBed.createComponent(ListadoCitasComponent);
    await fixture.whenStable();
    httpMock.expectOne('/api/citas').flush(citasMock);

    const citaAgendada = citasMock[0];
    expect(fixture.componentInstance.puedeCancelar(citaAgendada)).toBe(false);
    expect(fixture.componentInstance.puedeMarcarAtendida(citaAgendada)).toBe(true);
    expect(fixture.componentInstance.puedeMarcarNoAsistio(citaAgendada)).toBe(true);
    expect(fixture.componentInstance.puedeReprogramar(citaAgendada)).toBe(false);
  });

  it('CLIENTE solo debe poder cancelar y reprogramar sus propias citas', async () => {
    authServiceMock.rol.set('CLIENTE');
    authServiceMock.usuarioActual.set({ idPerfil: 'cli-1' });
    const fixture = TestBed.createComponent(ListadoCitasComponent);
    await fixture.whenStable();
    httpMock.expectOne('/api/citas').flush(citasMock);

    const citaAgendada = citasMock[0];
    expect(fixture.componentInstance.puedeCancelar(citaAgendada)).toBe(true);
    expect(fixture.componentInstance.puedeMarcarAtendida(citaAgendada)).toBe(false);
    expect(fixture.componentInstance.puedeReprogramar(citaAgendada)).toBe(true);
  });

  it('debe requerir motivo al cancelar (modal)', async () => {
    const fixture = TestBed.createComponent(ListadoCitasComponent);
    await fixture.whenStable();
    httpMock.expectOne('/api/citas').flush(citasMock);

    const cita = citasMock[0];
    fixture.componentInstance.abrirModalMotivo(cita, 'CANCELADA');
    await fixture.whenStable();

    expect(fixture.componentInstance.motivoModalAbierto()).toBe(true);
    expect(fixture.componentInstance.esMotivoObligatorio('CANCELADA')).toBe(true);
    expect(fixture.componentInstance.esMotivoObligatorio('ATENDIDA')).toBe(false);
  });

  it('debe mostrar error 409 al cambiar estado', async () => {
    const fixture = TestBed.createComponent(ListadoCitasComponent);
    await fixture.whenStable();
    httpMock.expectOne('/api/citas').flush(citasMock);

    const cita = citasMock[0];
    fixture.componentInstance.abrirModalMotivo(cita, 'CANCELADA');
    fixture.componentInstance.motivoModalTexto.set('Motivo de prueba');
    fixture.componentInstance.confirmarCambioEstado();

    const req = httpMock.expectOne('/api/citas/cita-1/estado');
    expect(req.request.method).toBe('PATCH');
    req.flush(
      { mensaje: 'Solo puedes cancelar tu cita con al menos 24 horas de anticipacion' },
      { status: 409, statusText: 'Conflict' }
    );

    expect(notificacionSpy.error).toHaveBeenCalledWith(
      'Solo puedes cancelar tu cita con al menos 24 horas de anticipacion'
    );
  });
});