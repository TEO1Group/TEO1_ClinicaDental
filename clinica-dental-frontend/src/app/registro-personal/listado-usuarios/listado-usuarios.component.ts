import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { UsuarioSistemaService } from '../service/usuario-sistema.service';
import { UsuarioListado } from '../models/usuario-listado.model';

@Component({
  selector: 'app-listado-usuarios',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './listado-usuarios.component.html',
  styleUrl: './listado-usuarios.component.scss'
})
export class ListadoUsuariosComponent implements OnInit {
  private readonly usuarioSistemaService = inject(UsuarioSistemaService);
  private readonly formBuilder = inject(FormBuilder);

  readonly roles = [
    { valor: '', etiqueta: 'Todos' },
    { valor: 'ADMIN', etiqueta: 'Administrador' },
    { valor: 'DOCTOR', etiqueta: 'Doctor' },
    { valor: 'SECRETARIA', etiqueta: 'Secretaria' },
    { valor: 'CLIENTE', etiqueta: 'Cliente' }
  ];

  readonly filtroForm = this.formBuilder.nonNullable.group({
    rol: ['']
  });

  private readonly _usuarios = signal<UsuarioListado[]>([]);
  private readonly _cargando = signal<boolean>(false);
  private readonly _error = signal<string>('');

  readonly usuarios = this._usuarios.asReadonly();
  readonly cargando = this._cargando.asReadonly();
  readonly error = this._error.asReadonly();

  readonly usuariosFiltrados = computed(() => {
    const rolFiltro = this.filtroForm.controls.rol.value;
    const lista = this._usuarios();

    if (!rolFiltro) {
      return lista;
    }
    return lista.filter(u => u.rol === rolFiltro);
  });

  ngOnInit(): void {
    this.cargarUsuarios();
  }

  cargarUsuarios(): void {
    this._cargando.set(true);
    this._error.set('');

    this.usuarioSistemaService.listarUsuarios().subscribe({
      next: (response) => {
        this._usuarios.set(response);
        this._cargando.set(false);
      },
      error: (error) => {
        this._cargando.set(false);
        this._error.set(error.error?.mensaje || 'Error al cargar los usuarios.');
      }
    });
  }

  // TODO: Implementar cuando el backend lo defina
  editarUsuario(usuario: UsuarioListado): void {
    console.log('Editar usuario:', usuario);
  }

  // TODO: Implementar cuando el backend lo defina
  cambiarEstado(usuario: UsuarioListado): void {
    console.log('Cambiar estado del usuario:', usuario);
  }
}