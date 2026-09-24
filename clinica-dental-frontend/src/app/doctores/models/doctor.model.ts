export interface DoctorResponse {
  idDoctor: string;
  idUsuario: string;
  nombre: string;
  apellido: string;
  email: string;
  telefono: string | null;
  especialidad: string;
  numeroColegiado: string | null;
  numeroClinica: string | null;
  calificacionPromedio: number;
}

export interface DoctorUpdateRequest {
  nombre?: string;
  apellido?: string;
  telefono?: string;
  especialidad: string;
  numeroColegiado?: string;
  numeroClinica?: string;
}