import { Component, signal, inject } from '@angular/core';
import { RouterOutlet, Router, NavigationEnd } from '@angular/router';
import { CommonModule } from '@angular/common';
import { filter, map } from 'rxjs';
import { toSignal } from '@angular/core/rxjs-interop';
import { AuthService } from './core/services/auth.service';
import { HeaderComponent } from './header/header.component';

@Component({
  imports: [RouterOutlet, CommonModule, HeaderComponent],
  selector: 'app-root',
  styleUrl: './app.scss',
  templateUrl: './app.html',
})
export class App {
  protected readonly title = signal('clinica-dental-frontend');

  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  private readonly rutasSinHeader = ['/', '/registro'];

  readonly rutaActual = toSignal(
    this.router.events.pipe(
      filter((event): event is NavigationEnd => event instanceof NavigationEnd),
      map((event) => event.urlAfterRedirects)
    ),
    { initialValue: this.router.url }
  );

  readonly mostrarHeader = signal(false);

  constructor() {
    this.authService.cargarTokenDesdeStorage();
  }

  ngOnInit(): void {
    this.mostrarHeader.set(!this.rutasSinHeader.includes(this.router.url));
    this.actualizarVisibilidadHeader();
  }

  private actualizarVisibilidadHeader(): void {
    this.router.events
      .pipe(filter((event): event is NavigationEnd => event instanceof NavigationEnd))
      .subscribe((event) => {
        const url = event.urlAfterRedirects;
        this.mostrarHeader.set(!this.rutasSinHeader.includes(url));
      });
  }
}