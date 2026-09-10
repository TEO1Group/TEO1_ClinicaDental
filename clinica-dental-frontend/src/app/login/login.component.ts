import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly loginForm = this.formBuilder.nonNullable.group({
    usuario: ['', [Validators.required]],
    password: ['', [Validators.required]]
  });

  isSubmitting = false;
  showPassword = false;
  successMessage = '';
  errorMessage = '';

  togglePasswordVisibility(): void {
    this.showPassword = !this.showPassword;
  }

  onSubmit(): void {
    this.successMessage = '';
    this.errorMessage = '';

    if (this.loginForm.invalid) {
      this.loginForm.markAllAsTouched();
      return;
    }

    this.isSubmitting = true;
    this.authService.login(this.loginForm.getRawValue()).subscribe({
      next: ({ token }) => {
        this.authService.guardarSesion({ token });
        this.successMessage = 'Inicio de sesión correcto.';
        this.isSubmitting = false;
        this.router.navigate(['/dashboard']);
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = error.error?.mensaje || error.error?.message || 'Usuario o contraseña incorrectos.';
        this.isSubmitting = false;
      }
    });
  }
}
