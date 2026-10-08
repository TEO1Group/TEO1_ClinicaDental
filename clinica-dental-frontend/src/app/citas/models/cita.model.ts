export type EstadoCita = 'AGENDADA' | 'ATENDIDA' | 'CANCELADA' | 'NO_ASISTIO';

export interface CitaResponse {
  idCita: string;
  idCliente: string;
  idDoctor: string;
  fecha: string;
  hora: string;
  estado: EstadoCita;
  notas: string | null;
  nombreCliente?: string;
  apellidoCliente?: string;
  nombreDoctor?: string;
  apellidoDoctor?: string;
  nombrePaciente?: string;
  apellidoPaciente?: string;
}

export interface CitaRequest {
  idDoctor: string;
  idCliente?: string;
  fecha: string;
  hora: string;
  notas?: string;
}

export interface EstadoCitaRequest {
  estado: EstadoCita;
}

export type CambioEstadoCitaRequest = EstadoCitaRequest;

export interface CitaFiltros {
  estado?: EstadoCita;
  fecha?: string;
  idDoctor?: string;
  idCliente?: string;
}
