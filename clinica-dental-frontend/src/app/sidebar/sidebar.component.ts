import { Component, Input, Output, EventEmitter, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../core/services/auth.service';

interface ItemSidebar {
  label: string;
  icono: string;
  ruta: string;
  roles: string[];
}

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  templateUrl: './sidebar.component.html',
  styleUrl: './sidebar.component.scss'
})
export class SidebarComponent {
  private readonly authService = inject(AuthService);

  @Input() visible = false;
  @Output() cerrar = new EventEmitter<void>();

  readonly rol = this.authService.rol;

  readonly items: ItemSidebar[] = [
    {
      label: 'Dashboard',
      icono: 'bi-speedometer2',
      ruta: '/dashboard',
      roles: ['ADMIN', 'DOCTOR', 'SECRETARIA', 'CLIENTE']
    },
    {
      label: 'Usuarios',
      icono: 'bi-people',
      ruta: '/admin/usuarios',
      roles: ['ADMIN']
    },
    {
      label: 'Doctores',
      icono: 'bi-person-badge',
      ruta: '/doctores',
      roles: ['ADMIN', 'DOCTOR', 'SECRETARIA']
    },
    // TODO: Descomentar cuando el módulo de pacientes esté listo
    // {
    //   label: 'Pacientes',
    //   icono: 'bi-person-vcard',
    //   ruta: '/pacientes',
    //   roles: ['ADMIN', 'DOCTOR', 'SECRETARIA']
    // }
  ];

  itemsVisibles(): ItemSidebar[] {
    const rolActual = this.rol();
    if (!rolActual) return [];
    return this.items.filter(item => item.roles.includes(rolActual));
  }

  cerrarSidebar(): void {
    this.cerrar.emit();
  }
}