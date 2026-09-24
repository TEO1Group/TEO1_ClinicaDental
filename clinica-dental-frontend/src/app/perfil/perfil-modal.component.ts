import { Component, Input, Output, EventEmitter, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AuthService } from '../core/services/auth.service';

@Component({
  selector: 'app-perfil-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './perfil-modal.component.html',
  styleUrl: './perfil-modal.component.scss'
})
export class PerfilModalComponent {
  private readonly authService = inject(AuthService);

  @Input() visible = false;
  @Output() cerrar = new EventEmitter<void>();

  readonly usuarioActual = this.authService.usuarioActual;
  readonly rol = this.authService.rol;

  cerrarModal(): void {
    this.cerrar.emit();
  }
}