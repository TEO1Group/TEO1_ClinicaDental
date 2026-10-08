import { Component, HostListener, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CitaService } from '../citas/service/cita.service';
import { CitaResponse } from '../citas/models/cita.model';
import { AuthService } from '../core/services/auth.service';

@Component({
  selector: 'app-campana',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './campana.component.html',
  styleUrl: './campana.component.scss'
})
export class CampanaComponent {
  private readonly citaService = inject(CitaService);
  private readonly authService = inject(AuthService);

  readonly proximas = this.citaService.proximasSignal;
  readonly rol = this.authService.rol;

  panelAbierto = signal(false);

  togglePanel(event: Event): void {
    event.stopPropagation();
    this.panelAbierto.update(v => !v);
  }

  cerrarPanel(): void {
    this.panelAbierto.set(false);
  }

  @HostListener('document:click')
  cerrarAlClickFuera(): void {
    this.panelAbierto.set(false);
  }

  formatearHora(hora: string): string {
    return hora.slice(0, 5);
  }

  formatearFecha(fecha: string): string {
    const [year, month, day] = fecha.split('-');
    return `${day}/${month}/${year}`;
  }

  descripcionCita(cita: CitaResponse): string {
    const rolActual = this.rol();
    const nombreCliente = [cita.nombreCliente ?? cita.nombrePaciente, cita.apellidoCliente ?? cita.apellidoPaciente]
      .filter(Boolean).join(' ') || cita.idCliente;
    const nombreDoctor = [cita.nombreDoctor, cita.apellidoDoctor].filter(Boolean).join(' ') || cita.idDoctor;

    if (rolActual === 'CLIENTE') {
      return `Dr. ${nombreDoctor}`;
    }
    if (rolActual === 'DOCTOR') {
      return nombreCliente;
    }
    // SECRETARIA / ADMIN
    return `${nombreCliente} con Dr. ${nombreDoctor}`;
  }
}
