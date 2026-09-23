import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NotificacionService } from '../service/notificacion.service';

@Component({
  selector: 'app-notificacion',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './notificacion.component.html',
  styleUrl: './notificacion.component.scss'
})
export class NotificacionComponent {
  private readonly notificacionService = inject(NotificacionService);

  readonly notificaciones = this.notificacionService.notificaciones;

  cerrar(id: number): void {
    this.notificacionService.cerrar(id);
  }

  iconoPorTipo(tipo: string): string {
    const iconos: Record<string, string> = {
      success: 'bi-check-circle-fill',
      error: 'bi-exclamation-triangle-fill',
      info: 'bi-info-circle-fill'
    };
    return iconos[tipo] || 'bi-info-circle-fill';
  }
}