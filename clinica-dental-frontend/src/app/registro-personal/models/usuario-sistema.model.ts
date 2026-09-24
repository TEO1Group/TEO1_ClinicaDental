export interface UsuarioSistemaRequest {
  nombre: string;
  apellido: string;
  email: string;
  password: string;
  telefono?: string;
  idRol: string;

  // Solo si el rol es DOCTOR
  especialidad?: string;
  numeroColegiado?: string;
  numeroClinica?: string;

  // Solo si el rol es SECRETARIA
  turno?: string;
  area?: string;
}

export interface UsuarioSistemaResponse {
  mensaje: string;
  id: string;
}

export interface UsuarioUpdateRequest {
  nombre?: string;
  apellido?: string;
  email?: string;
  telefono?: string;
}

export interface EstadoUsuarioRequest {
  estado: 'ACTIVO' | 'INACTIVO';
}