import { Component, inject } from '@angular/core';
import { FormBuilder, Validators, FormGroup, ReactiveFormsModule, AbstractControl, ValidationErrors } from '@angular/forms';
import { CommonModule, NgClass } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../../core/services/auth.service';
import { PacienteRegistroRequest } from '../../models/paciente-registro.model';

@Component({
  selector: 'app-formulario-registro-cliente',
  standalone: true,
  imports: [ ReactiveFormsModule, CommonModule, NgClass],
  templateUrl: './formulario-registro.component.html',
  styleUrls: ['./formulario-registro.component.scss']
})
export class FormularioRegistroClienteComponent {
  private readonly authService = inject(AuthService);
  registroForm: FormGroup;

  showPassword = false;
  showConfirmPassword = false;
  isSubmitting = false;
  successMessage = '';
  errorMessage = '';

  constructor(private fb: FormBuilder) {
    this.registroForm = this.fb.nonNullable.group({
      dpi: ['', [Validators.required, Validators.pattern(/^\d{13}$/)]],
      nombre: ['', [Validators.required, Validators.minLength(3)]],
      password: ['', [
        Validators.required,
        Validators.minLength(8),
        Validators.pattern(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9\s]).{8,}$/)
      ]],
      confirmarPassword: ['', [Validators.required]],
      celular: ['', [Validators.required, Validators.pattern(/^\d{1,15}$/)]]
    }, { validators: this.passwordsIguales });
  }

  private passwordsIguales(group: AbstractControl): ValidationErrors | null {
    const pass = group.get('password')?.value;
    const confirm = group.get('confirmarPassword')?.value;
    return pass === confirm ? null : { passwordMismatch: true };
  }

  onSubmit(): void {
    this.successMessage = '';
    this.errorMessage = '';

    if (this.registroForm.invalid) {
      this.registroForm.markAllAsTouched();
      return;
    }

    const { dpi, nombre, celular, password } = this.registroForm.getRawValue();
    const datosRegistro: PacienteRegistroRequest = { dpi, nombre, celular, password };
    this.isSubmitting = true;

    this.authService.registrar(datosRegistro).subscribe({
      next: (response) => {
        this.successMessage = response.mensaje || 'Registro completado correctamente.';
        this.registroForm.reset();
        this.isSubmitting = false;
      },
      error: (error: HttpErrorResponse) => {
        this.errorMessage = this.getRegistrationError(error);
        this.isSubmitting = false;
      }
    });
  }

  private getRegistrationError(error: HttpErrorResponse): string {
    const backendMessage = typeof error.error === 'string'
      ? error.error
      : error.error?.message || error.error?.detail || '';

    if (error.status === 409 && backendMessage.toLowerCase().includes('dpi')) {
      return 'El DPI ya está registrado.';
    }

    if (error.status === 409 && (backendMessage.toLowerCase().includes('celular') || backendMessage.toLowerCase().includes('teléfono'))) {
      return 'El celular ya está registrado.';
    }

    return backendMessage || 'No se pudo completar el registro. Intenta nuevamente.';
  }

  togglePassword(): void {
    this.showPassword = !this.showPassword;
  }

  toggleConfirmPassword(): void {
    this.showConfirmPassword = !this.showConfirmPassword;
  }
}