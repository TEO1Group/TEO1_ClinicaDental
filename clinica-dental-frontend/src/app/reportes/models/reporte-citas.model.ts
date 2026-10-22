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

export interface ReporteHorasDemandadasResponse {
  desde: string;
  hasta: string;
  horas: { hora: number; cantidad: number }[];
}

export interface ReporteInasistenciasResponse {
  desde: string;
  hasta: string;
  totalNoAsistio: number;
  totalCanceladas: number;
  total: number;
  doctores: {
    idDoctor: string;
    nombreDoctor: string;
    apellidoDoctor: string;
    noAsistio: number;
    canceladas: number;
    total: number;
  }[];
  pacientes: {
    idCliente: string;
    nombreCliente: string;
    apellidoCliente: string;
    noAsistio: number;
    canceladas: number;
    total: number;
  }[];
}

export interface ReporteIngresosTratamientoResponse {
  desde: string;
  hasta: string;
  cantidadTotal: number;
  ingresoTotal: number;
  tratamientos: {
    idTratamiento: string;
    nombre: string;
    cantidad: number;
    total: number;
  }[];
}