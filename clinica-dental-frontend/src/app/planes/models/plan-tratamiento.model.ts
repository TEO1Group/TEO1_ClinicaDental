import { CitaResponse } from '../../citas/models/cita.model';

export type EstadoPlan = 'ACTIVO' | 'COMPLETADO' | 'CANCELADO';

export interface PlanTratamientoResponse {
  idPlan: string;
  idCliente: string;
  nombreCliente: string;
  apellidoCliente: string;
  idDoctor: string;
  nombreDoctor: string;
  apellidoDoctor: string;
  idTratamiento: string;
  nombreTratamiento: string;
  estado: EstadoPlan;
  fechaCreacion: string;
  totalSesiones: number;
  sesionesAtendidas: number;
  sesionesAgendadas: number;
  sesionesPendientes: number;
  porcentajeAvance: number;
  costoTotal: number;
  totalAbonado: number;
  saldo: number;
  citas?: CitaResponse[];
}

export interface PlanTratamientoRequest {
  idCliente: string;
  idDoctor?: string;
  idTratamiento: string;
  totalSesiones: number;
  costoTotal?: number;
}

export interface SesionPlanRequest {
  fecha: string;
  hora: string;
  notas?: string;
}