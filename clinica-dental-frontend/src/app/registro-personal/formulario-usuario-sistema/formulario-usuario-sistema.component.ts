import { Component, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { Router } from '@angular/router';
import { UsuarioSistemaService, Rol } from '../service/usuario-sistema.service';
import { UsuarioSistemaRequest } from '../models/usuario-sistema.model';

@Component({
  selector: 'app-formulario-usuario-sistema',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './formulario-usuario-sistema.component.html',
  styleUrl: './formulario-usuario-sistema.component.scss'
})
export class FormularioUsuarioSistemaComponent implements OnInit {
  private readonly formBuilder = inject(FormBuilder);
  private readonly usuarioSistemaService = inject(UsuarioSistemaService);
  private readonly router = inject(Router);

  roles = signal<Rol[]>([]);

  readonly turnos = [
    { valor: 'MANANA', etiqueta: 'Mañana' },
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
      Validators.pattern(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9\s]).+$/)
    ]],
    confirmarPassword: ['', [Validators.required]],
    telefono: ['', [Validators.pattern(/^[0-9]{8}$/)]],
    idRol: ['', [Validators.required]],

    // Doctor
    especialidad: [''],
    numeroColegiado: [''],
    numeroClinica: [''],

    // Secretaria
    turno: [''],
    area: ['']
  }, { validators: this.passwordsIguales });

  ngOnInit(): void {
    this.cargarRoles();
    this.configurarValidacionPorRol();
  }

  private passwordsIguales(group: AbstractControl): ValidationErrors | null {
    const pass = group.get('password')?.value;
    const confirm = group.get('confirmarPassword')?.value;
    return pass === confirm ? null : { passwordMismatch: true };
  }

  private cargarRoles(): void {
    this.usuarioSistemaService.obtenerRoles().subscribe({
      next: (roles) => {
        this.roles.set(roles.filter(r => r.nombreRol !== 'CLIENTE'));
      },
      error: (err) => {

        this.errorMessage = 'Error al cargar los roles.';
      }
    });
  }

  private configurarValidacionPorRol(): void {
    this.usuarioForm.get('idRol')?.valueChanges.subscribe((rol) => {
      const especialidadControl = this.usuarioForm.get('especialidad');

      if (rol === 'DOCTOR') {
        especialidadControl?.setValidators([Validators.required]);
      } else {
        especialidadControl?.clearValidators();
      }
      especialidadControl?.updateValueAndValidity();
    });
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
      telefono: valores.telefono || undefined,
      idRol: valores.idRol
    };

    if (valores.idRol === 'DOCTOR') {
      request.especialidad = valores.especialidad;
      request.numeroColegiado = valores.numeroColegiado || undefined;
      request.numeroClinica = valores.numeroClinica || undefined;
    }

    if (valores.idRol === 'SECRETARIA') {
      request.turno = valores.turno || undefined;
      request.area = valores.area || undefined;
    }

    return request;
  }
}