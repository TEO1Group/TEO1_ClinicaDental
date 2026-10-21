export interface ClienteResponse {
  idCliente: string;
  idUsuario: string;
  nombre: string;
  apellido: string;
  email: string;
  telefono: string | null;
  dpi: string;
  direccion: string | null;
  fechaNacimiento: string | null;
  enListaNegra: boolean;
  motivoListaNegra: string | null;
  estado: string;
}

export interface ClienteUpdateRequest {
  telefono?: string | null;
  direccion?: string | null;
  fechaNacimiento?: string | null;
}

export interface ListaNegraRequest {
  enListaNegra: boolean;
  motivo?: string | null;
}

export interface LevantamientoInasistenciaResponse {
  idLevantamiento: string;
  motivo: string;
  inasistencias: number;
  idLevantadoPor: string | null;
  fecha: string;
}

export interface InasistenciasPacienteResponse {
  idCliente: string;
  inasistencias: number;
  limite: number;
  bloqueado: boolean;
  ultimoLevantamiento: LevantamientoInasistenciaResponse | null;
}

export interface LevantamientoInasistenciaRequest {
  motivo: string;
}