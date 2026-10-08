export type EstadoCita = 'AGENDADA' | 'ATENDIDA' | 'CANCELADA' | 'NO_ASISTIO';

export interface CitaResponse {
  idCita: string;
  idCliente: string;
  idDoctor: string;
  fecha: string;
  hora: string;
  estado: EstadoCita;
  notas: string | null;
  nombreDoctor?: string;
  apellidoDoctor?: string;
  nombrePaciente?: string;
  apellidoPaciente?: string;
}

export interface EstadoCitaRequest {
  estado: EstadoCita;
}