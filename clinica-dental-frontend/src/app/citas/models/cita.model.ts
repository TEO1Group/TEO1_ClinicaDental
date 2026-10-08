export type EstadoCita = 'AGENDADA' | 'COMPLETADA' | 'CANCELADA' | 'INASISTENCIA' | 'APLAZADA';

//para crear una cita
export interface CitaRequest {
  idCliente: string;
  idDoctor: string;
  fecha: string;      // "yyyy-MM-dd"
  hora: string;       // "HH:mm"
  notas?: string;
}

// Respuesta del backend al consultar una cita
export interface CitaResponse {
  idCita: string;
  idCliente: string;
  idDoctor: string;
  nombreCliente: string;
  apellidoCliente: string;
  nombreDoctor: string;
  apellidoDoctor: string;
  fecha: string;      // "yyyy-MM-dd"
  hora: string;       // "HH:mm:ss" (se formatea en el frontend)
  estado: EstadoCita;
  notas: string | null;
}

export interface CambioEstadoCitaRequest {
  estado: EstadoCita;
}

export interface CitaFiltros {
  estado?: EstadoCita;
  fecha?: string;      // "yyyy-MM-dd"
  idDoctor?: string;
  idCliente?: string;
}