export interface TratamientoResponse {
  idTratamiento: string;
  nombre: string;
  descripcion: string | null;
  duracionMinutos: number;
  costo: number;
}

export interface CitaTratamientoRequest {
  idTratamiento: string;
  costo?: number;
  observaciones?: string;
}

export interface CitaTratamientoResponse {
  idCitaTratamiento: string;
  idCita: string;
  fechaCita: string;
  horaCita: string;
  nombreDoctor: string;
  apellidoDoctor: string;
  idTratamiento: string;
  nombreTratamiento: string;
  costo: number;
  observaciones: string | null;
  fechaRegistro: string;
}

export interface TratamientosPacienteResponse {
  total: number;
  tratamientos: CitaTratamientoResponse[];
}
