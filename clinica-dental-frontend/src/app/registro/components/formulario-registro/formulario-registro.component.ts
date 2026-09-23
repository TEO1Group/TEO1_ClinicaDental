import { Component, inject } from '@angular/core';
import { FormBuilder, Validators, FormGroup, ReactiveFormsModule, AbstractControl, ValidationErrors } from '@angular/forms';
import { CommonModule, NgClass } from '@angular/common';
import { Router } from '@angular/router';
import { PacienteRegistroRequest } from '../../models/paciente-registro.model';
import { AuthService } from '../../../core/services/auth.service';

@Component({
  selector: 'app-formulario-registro-cliente',
  standalone: true,
  imports: [ReactiveFormsModule, CommonModule, NgClass],
  templateUrl: './formulario-registro.component.html',
  styleUrls: ['./formulario-registro.component.scss']
})
export class FormularioRegistroClienteComponent {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  registroForm: FormGroup;

  showPassword = false;
  showConfirmPassword = false;
  isSubmitting = false;
  successMessage = '';
  errorMessage = '';

  constructor() {
    this.registroForm = this.fb.nonNullable.group({
      dpi: ['', [Validators.required, Validators.pattern(/^\d{13}$/)]],
      nombre: ['', [Validators.required, Validators.minLength(3)]],
      apellido: ['', [Validators.required, Validators.minLength(3)]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [
        Validators.required,
        Validators.minLength(8),
        Validators.pattern(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9\s]).+$/)
      ]],
      confirmarPassword: ['', [Validators.required]],
      telefono: ['', [Validators.pattern(/^[0-9]{8}$/)]],
      direccion: [''],
      fechaNacimiento: ['']
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

    this.isSubmitting = true;

    const valores = this.registroForm.getRawValue();

    const datosRegistro: PacienteRegistroRequest = {
      dpi: valores.dpi,
      nombre: valores.nombre,
      apellido: valores.apellido,
      email: valores.email,
      password: valores.password,
      telefono: valores.telefono || undefined,
      direccion: valores.direccion || undefined,
      fechaNacimiento: valores.fechaNacimiento || undefined
    };

    this.authService.registro(datosRegistro).subscribe({
      next: (response) => {
        this.isSubmitting = false;
        this.successMessage = response.mensaje;
        this.registroForm.reset();
        setTimeout(() => {
          this.router.navigate(['']);
        }, 2000);
      },
      error: (error) => {
        this.isSubmitting = false;
        this.errorMessage = error.error?.mensaje || 'Error al registrarse. Intenta de nuevo.';
      }
    });
  }

  togglePassword(): void {
    this.showPassword = !this.showPassword;
  }

  toggleConfirmPassword(): void {
    this.showConfirmPassword = !this.showConfirmPassword;
  }
}