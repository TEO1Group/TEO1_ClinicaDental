export interface UsuarioSistemaRequest {
    nombre: string;
    apellido: string;
    email: string;
    password: string;
    telefono: string;
    idRol: string;

    //solo si el rol es de doctor
    especialidad?: string;
    numeroColegiado?: string;
    numeroClinica?: string;

    //solo es el rol es de secretaria
    turno?: string;
    area?: string;
}

export interface UsuarioSistemaResponse {
    mensaje: string;
    id: string;
}