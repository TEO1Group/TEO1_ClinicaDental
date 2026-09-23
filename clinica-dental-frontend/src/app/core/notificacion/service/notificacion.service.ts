import { Injectable, signal } from '@angular/core';

export type TipoNotificacion = 'success' | 'error' | 'info';

export interface Notificacion {
  id: number;
  tipo: TipoNotificacion;
  mensaje: string;
}

@Injectable({ providedIn: 'root' })
export class NotificacionService {
  private readonly _notificaciones = signal<Notificacion[]>([]);
  readonly notificaciones = this._notificaciones.asReadonly();

  private contador = 0;

  mostrar(mensaje: string, tipo: TipoNotificacion = 'info', duracionMs = 4000): void {
    const id = ++this.contador;
    const nueva: Notificacion = { id, tipo, mensaje };

    this._notificaciones.update(lista => [...lista, nueva]);

    setTimeout(() => {
      this.cerrar(id);
    }, duracionMs);
  }

  exito(mensaje: string): void {
    this.mostrar(mensaje, 'success');
  }

  error(mensaje: string): void {
    this.mostrar(mensaje, 'error');
  }

  info(mensaje: string): void {
    this.mostrar(mensaje, 'info');
  }

  cerrar(id: number): void {
    this._notificaciones.update(lista => lista.filter(n => n.id !== id));
  }
}