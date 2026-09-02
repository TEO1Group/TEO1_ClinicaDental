export interface PacienteRegistroRequest {
  nombre: string;
  apellido: string;
  email: string;
  password: string;
  confirmarPassword?: string;
  telefono?: string;
  direccion: string;
  fechaNacimiento: string;
}