import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { Router } from '@angular/router';
import { UsuarioSistemaService } from '../service/usuario-sistema.service';
import { UsuarioSistemaRequest } from '../models/usuario-sistema.model';

@Component({
  selector: 'app-formulario-usuario-sistema',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './formulario-usuario-sistema.component.html',
  styleUrl: './formulario-usuario-sistema.component.scss'
})
export class FormularioUsuarioSistemaComponent {
  private readonly formBuilder = inject(FormBuilder);
  private readonly usuarioSistemaService = inject(UsuarioSistemaService);
  private readonly router = inject(Router);

  readonly roles = [
    { valor: 'DOCTOR', etiqueta: 'Doctor' },
    { valor: 'SECRETARIA', etiqueta: 'Secretaria' },
    { valor: 'ADMIN', etiqueta: 'Administrador' }
  ];

  readonly turnos = [
    { valor: 'MAÑANA', etiqueta: 'Mañana' },
    { valor: 'TARDE', etiqueta: 'Tarde' },
    { valor: 'NOCHE', etiqueta: 'Noche' }
  ];

  isSubmitting = false;
  showPassword = false;
  showConfirmPassword = false;
  successMessage = '';
  errorMessage = '';

  readonly usuarioForm = this.formBuilder.nonNullable.group({
    nombre: ['', [Validators.required, Validators.minLength(3)]],
    apellido: ['', [Validators.required, Validators.minLength(3)]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [
      Validators.required,
      Validators.minLength(8),
      Validators.pattern(/^(?=.*[a-zA-Z])(?=.*\d)(?=.*[@#$%^&+=!._*-]).{8,}$/)
    ]],
    confirmarPassword: ['', [Validators.required]],
    telefono: ['', [Validators.required, Validators.pattern(/^[0-9]{8}$/)]],
    idRol: ['', [Validators.required]],

    // Doctor
    especialidad: [''],
    numeroColegiado: [''],
    numeroClinica: [''],

    // Secretaria
    turno: [''],
    area: ['']
  }, { validators: this.passwordsIguales });

  private passwordsIguales(group: AbstractControl): ValidationErrors | null {
    const pass = group.get('password')?.value;
    const confirm = group.get('confirmarPassword')?.value;
    return pass === confirm ? null : { passwordMismatch: true };
  }

  get rolSeleccionado(): string {
    return this.usuarioForm.get('idRol')?.value || '';
  }

  togglePassword(): void {
    this.showPassword = !this.showPassword;
  }

  toggleConfirmPassword(): void {
    this.showConfirmPassword = !this.showConfirmPassword;
  }

  onSubmit(): void {
    this.successMessage = '';
    this.errorMessage = '';

    if (this.usuarioForm.invalid) {
      this.usuarioForm.markAllAsTouched();
      return;
    }

    this.isSubmitting = true;

    const request = this.construirRequest();

    this.usuarioSistemaService.crearUsuario(request).subscribe({
      next: (response) => {
        this.isSubmitting = false;
        this.successMessage = response.mensaje;
        this.usuarioForm.reset();
        this.router.navigate(['/admin/usuarios']);
      },
      error: (error) => {
        this.isSubmitting = false;
        this.errorMessage = error.error?.mensaje || 'Error al crear el usuario.';
      }
    });
  }

  private construirRequest(): UsuarioSistemaRequest {
    const valores = this.usuarioForm.getRawValue();

    const request: UsuarioSistemaRequest = {
      nombre: valores.nombre,
      apellido: valores.apellido,
      email: valores.email,
      password: valores.password,
      telefono: valores.telefono,
      idRol: valores.idRol
    };

    if (valores.idRol === 'DOCTOR') {
      request.especialidad = valores.especialidad;
      request.numeroColegiado = valores.numeroColegiado;
      request.numeroClinica = valores.numeroClinica;
    }

    if (valores.idRol === 'SECRETARIA') {
      request.turno = valores.turno;
      request.area = valores.area;
    }

    return request;
  }
}