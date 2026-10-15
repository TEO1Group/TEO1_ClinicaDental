export interface CitasPorDoctorResponse {
  idDoctor: string;
  nombreDoctor: string;
  apellidoDoctor: string;
  especialidad: string;
  agendadas: number;
  atendidas: number;
  canceladas: number;
  noAsistio: number;
  total: number;
}

export interface ReporteCitasPorDoctorResponse {
  desde: string;
  hasta: string;
  doctores: CitasPorDoctorResponse[];
}