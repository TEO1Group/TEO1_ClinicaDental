import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { AuthService } from './core/services/auth.service';

@Component({
  imports: [RouterOutlet],
  selector: 'app-root',
  styleUrl: './app.scss',
  templateUrl: './app.html',
})
export class App {
  protected readonly title = signal('clinica-dental-frontend');

  constructor(private authService: AuthService) {}

  ngOnInit(): void {
    this.authService.cargarTokenDesdeStorage();
  }
}
