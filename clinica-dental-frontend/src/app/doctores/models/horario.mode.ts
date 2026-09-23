export interface HorarioResponse {
  idHorario: string;
  idDoctor: string;
  diaSemana: string;
  horaInicio: string;
  horaFin: string;
}

export interface HorarioRequest {
  diaSemana: string;
  horaInicio: string;
  horaFin: string;
}