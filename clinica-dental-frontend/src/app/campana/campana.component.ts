import { Component, HostListener, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { CitaService } from '../citas/service/cita.service';
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

  descripcionCita(cita: { nombreCliente: string; apellidoCliente: string; nombreDoctor: string; apellidoDoctor: string }): string {
    const rolActual = this.rol();

    if (rolActual === 'CLIENTE') {
      return `Dr. ${cita.nombreDoctor} ${cita.apellidoDoctor}`;
    }
    if (rolActual === 'DOCTOR') {
      return `${cita.nombreCliente} ${cita.apellidoCliente}`;
    }
    // SECRETARIA / ADMIN
    return `${cita.nombreCliente} ${cita.apellidoCliente} con Dr. ${cita.nombreDoctor} ${cita.apellidoDoctor}`;
  }
}