import { Component, Output, EventEmitter, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../core/services/auth.service';

@Component({
  selector: 'app-header',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './header.component.html',
  styleUrl: './header.component.scss'
})
export class HeaderComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  @Output() toggleSidebar = new EventEmitter<void>();

  readonly rol = this.authService.rol;

  toggle(): void {
    this.toggleSidebar.emit();
  }

  logout(): void {
    this.authService.limpiarSesion();
    this.router.navigate(['']);
  }
}