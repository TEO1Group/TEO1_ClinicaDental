export interface HistorialClinicoRequest {
  descripcion: string;
}

export interface HistorialClinicoResponse {
  idCliente?: string;
  idHistorial: string;
  fecha: string;
  descripcion: string;
}
