import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MisTratamientosService } from '../../service/mis-tratamientos.service';
import {
  TratamientosPacienteResponse,
  CitaTratamientoResponse
} from '../../../pacientes/models/tratamiento.model';

interface CitaAgrupada {
  idCita: string;
  fechaCita: string;
  horaCita: string;
  nombreDoctor: string;
  apellidoDoctor: string;
  tratamientos: CitaTratamientoResponse[];
  subtotal: number;
}

@Component({
  selector: 'app-mis-tratamientos',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './mis-tratamientos.component.html',
  styleUrl: './mis-tratamientos.component.scss'
})
export class MisTratamientosComponent implements OnInit {
  private readonly misTratamientosService = inject(MisTratamientosService);

  private readonly _data = signal<TratamientosPacienteResponse | null>(null);
  private readonly _cargando = signal<boolean>(false);
  private readonly _error = signal<string>('');

  readonly data = this._data.asReadonly();
  readonly cargando = this._cargando.asReadonly();
  readonly error = this._error.asReadonly();

  readonly citasAgrupadas = computed<CitaAgrupada[]>(() => {
    const data = this._data();
    if (!data) return [];

    const mapa = new Map<string, CitaAgrupada>();

    for (const t of data.tratamientos) {
      if (!mapa.has(t.idCita)) {
        mapa.set(t.idCita, {
          idCita: t.idCita,
          fechaCita: t.fechaCita,
          horaCita: t.horaCita,
          nombreDoctor: t.nombreDoctor,
          apellidoDoctor: t.apellidoDoctor,
          tratamientos: [],
          subtotal: 0
        });
      }
      const cita = mapa.get(t.idCita)!;
      cita.tratamientos.push(t);
      cita.subtotal += t.costo;
    }

    return Array.from(mapa.values());
  });

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this._cargando.set(true);
    this._error.set('');

    this.misTratamientosService.listarMisTratamientos().subscribe({
      next: (response) => {
        this._data.set(response);
        this._cargando.set(false);
      },
      error: (error) => {
        this._cargando.set(false);
        if (error.status === 403) return;
        this._error.set(error.error?.mensaje || 'Error al cargar tus tratamientos.');
      }
    });
  }

  formatearHora(hora: string): string {
    return hora.slice(0, 5);
  }

  nombreDoctor(cita: CitaAgrupada): string {
    return `Dr. ${cita.nombreDoctor} ${cita.apellidoDoctor}`;
  }

  formatearFecha(fecha: string): string {
    const [year, month, day] = fecha.split('-');
    return `${day}/${month}/${year}`;
  }
}