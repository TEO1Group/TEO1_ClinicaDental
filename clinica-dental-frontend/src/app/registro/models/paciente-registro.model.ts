export interface PacienteRegistroRequest {
  dpi: string;
  nombre: string;
  apellido: string;
  email: string;
  password: string;
  confirmarPassword?: string;
  telefono?: string;
  direccion: string;
  fechaNacimiento: string;
}

export interface RegistroResponse {
  mensaje: string;
  id: number;
}