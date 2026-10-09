import { Injectable, signal } from '@angular/core';

export type TipoNotificacion = 'success' | 'error' | 'info';

export interface Notificacion {
  id: number;
  tipo: TipoNotificacion;
  mensaje: string;
  centrada: boolean;
}

@Injectable({ providedIn: 'root' })
export class NotificacionService {
  private readonly _notificaciones = signal<Notificacion[]>([]);
  readonly notificaciones = this._notificaciones.asReadonly();

  private contador = 0;

  private mostrar(mensaje: string, tipo: TipoNotificacion, centrada: boolean, duracionMs = 4000): void {
    const id = ++this.contador;
    const nueva: Notificacion = { id, tipo, mensaje, centrada };

    this._notificaciones.update(lista => [...lista, nueva]);

    setTimeout(() => {
      this.cerrar(id);
    }, duracionMs);
  }

  exito(mensaje: string): void {
    this.mostrar(mensaje, 'success', false);
  }

  error(mensaje: string, centrada = false): void {
    this.mostrar(mensaje, 'error', centrada);
  }

  info(mensaje: string): void {
    this.mostrar(mensaje, 'info', false);
  }

  cerrar(id: number): void {
    this._notificaciones.update(lista => lista.filter(n => n.id !== id));
  }
}