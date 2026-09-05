import { Component } from '@angular/core';
import { FormBuilder, Validators, FormGroup, ReactiveFormsModule, AbstractControl, ValidationErrors } from '@angular/forms';
import { CommonModule, NgClass } from '@angular/common';
import { PacienteRegistroRequest } from '../../models/paciente-registro.model';

@Component({
  selector: 'app-formulario-registro-cliente',
  standalone: true,
  imports: [ ReactiveFormsModule, CommonModule, NgClass],
  templateUrl: './formulario-registro.component.html',
  styleUrls: ['./formulario-registro.component.scss']
})
export class FormularioRegistroClienteComponent {
  registroForm: FormGroup;

  showPassword = false;
  showConfirmPassword = false;

  constructor(private fb: FormBuilder) {
    this.registroForm = this.fb.nonNullable.group({
      dpi: ['', [Validators.required, Validators.pattern(/^\d{13}$/)]], //el dpi tiene que tener 13 digitos
      nombre: ['', [Validators.required, Validators.minLength(3)]],
      apellido: ['', [Validators.required, Validators.minLength(3)]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [
        Validators.required,
        Validators.minLength(8),
        // Expresión regular: Mínimo 8 caracteres, al menos una letra (mayuscula o minuscula), un numero y un caracter especial
        Validators.pattern(/^(?=.*[a-zA-Z])(?=.*\d)(?=.*[@#$%^&+=!._*-]).{8,}$/)
      ]],
      confirmarPassword: ['', [Validators.required]],
      telefono: ['', [Validators.required, Validators.pattern(/^[0-9]{8}$/)]],
      direccion: ['', [Validators.required]],
      fechaNacimiento: ['', [Validators.required]]
    }, { validators: this.passwordsIguales });
  }

  private passwordsIguales(group: AbstractControl): ValidationErrors | null {
    const pass = group.get('password')?.value;
    const confirm = group.get('confirmarPassword')?.value;
    return pass === confirm ? null : { passwordMismatch: true };
  }

  onSubmit(): void {
    if (this.registroForm.invalid) {
      this.registroForm.markAllAsTouched();
      return;
    }

    const datosRegistro: PacienteRegistroRequest = this.registroForm.getRawValue();
    console.log('Datos de registro tipados:', datosRegistro);
  }

  togglePassword(): void {
    this.showPassword = !this.showPassword;
  }

  toggleConfirmPassword(): void {
    this.showConfirmPassword = !this.showConfirmPassword;
  }
}