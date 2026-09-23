import { Component, signal, inject, OnInit } from '@angular/core';
import { RouterOutlet, Router, NavigationEnd } from '@angular/router';
import { CommonModule } from '@angular/common';
import { filter } from 'rxjs';
import { AuthService } from './core/services/auth.service';
import { HeaderComponent } from './header/header.component';
import { SidebarComponent } from './sidebar/sidebar.component';
import { NotificacionComponent } from './core/notificacion/notificacion-component/notificacion.component';

@Component({
  imports: [RouterOutlet, CommonModule, HeaderComponent, SidebarComponent, NotificacionComponent],
  selector: 'app-root',
  styleUrl: './app.scss',
  templateUrl: './app.html',
})
export class App implements OnInit {
  protected readonly title = signal('clinica-dental-frontend');

  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  private readonly rutasSinLayout = ['/', '/registro'];

  readonly mostrarLayout = signal(false);
  readonly sidebarVisible = signal(false);

  constructor() {
    this.authService.cargarTokenDesdeStorage();
  }

  ngOnInit(): void {
    this.actualizarLayout();
    this.router.events
      .pipe(filter((event): event is NavigationEnd => event instanceof NavigationEnd))
      .subscribe((event) => {
        this.actualizarLayoutPorUrl(event.urlAfterRedirects);
        this.sidebarVisible.set(false);
      });
  }

  toggleSidebar(): void {
    this.sidebarVisible.update(v => !v);
  }

  cerrarSidebar(): void {
    this.sidebarVisible.set(false);
  }

  private actualizarLayout(): void {
    this.actualizarLayoutPorUrl(this.router.url);
  }

  private actualizarLayoutPorUrl(url: string): void {
    const sinLayout = this.rutasSinLayout.includes(url);
    this.mostrarLayout.set(!sinLayout);
  }
}