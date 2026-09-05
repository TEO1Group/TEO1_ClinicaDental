import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent {
  private readonly formBuilder = inject(FormBuilder);

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
    this.errorMessage = 'El formulario está listo. El backend todavía debe exponer POST /auth/login para completar el ingreso.';
    this.isSubmitting = false;
  }
}