export interface BloqueoDoctorRequest {
  inicio: string;   // "yyyy-MM-ddTHH:mm:ss"
  fin: string;      // "yyyy-MM-ddTHH:mm:ss"
  motivo: string;   // max 200
}

export interface BloqueoDoctorResponse {
  idBloqueo: string;
  idDoctor: string;
  inicio: string;   // "yyyy-MM-ddTHH:mm:ss"
  fin: string;      // "yyyy-MM-ddTHH:mm:ss"
  motivo: string;
  idCreadoPor: string | null;
  fechaCreacion: string;
}

export interface BloqueoVigenteResponse {
  inicio: string;   // "yyyy-MM-ddTHH:mm:ss"
  fin: string;      // "yyyy-MM-ddTHH:mm:ss"
  motivo: string;
}
