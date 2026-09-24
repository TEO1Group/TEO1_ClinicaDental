export interface HistorialClinicoRequest {
  descripcion: string;
}

export interface HistorialClinicoResponse {
  idHistorial: string;
  idCliente: string;
  fecha: string;
  descripcion: string;
}