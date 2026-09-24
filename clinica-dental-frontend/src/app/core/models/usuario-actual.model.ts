export interface UsuarioActualResponse {
  idUsuario: string;
  nombre: string;
  apellido: string;
  email: string;
  telefono: string | null;
  rol: string;
  idPerfil: string | null;
}