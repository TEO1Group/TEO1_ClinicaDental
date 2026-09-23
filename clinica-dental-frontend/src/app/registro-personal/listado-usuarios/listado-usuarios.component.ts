import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { UsuarioSistemaService, Rol } from '../service/usuario-sistema.service';
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

  readonly roles = signal<Rol[]>([]);

  readonly filtroForm = this.formBuilder.nonNullable.group({
    rol: ['']
  });

  private readonly rolFiltro = toSignal(
    this.filtroForm.controls.rol.valueChanges,
    { initialValue: '' }
  );

  private readonly _usuarios = signal<UsuarioListado[]>([]);
  private readonly _cargando = signal<boolean>(false);
  private readonly _error = signal<string>('');

  readonly usuarios = this._usuarios.asReadonly();
  readonly cargando = this._cargando.asReadonly();
  readonly error = this._error.asReadonly();

  readonly usuariosFiltrados = computed(() => {
    const rol = this.rolFiltro();
    const lista = this._usuarios();

    if (!rol) {
      return lista;
    }
    return lista.filter(u => u.rol === rol);
  });

  ngOnInit(): void {
    this.cargarUsuarios();
    this.cargarRoles();
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

  private cargarRoles(): void {
    this.usuarioSistemaService.obtenerRoles().subscribe({
      next: (roles) => {
        this.roles.set(roles);
      },
      error: () => {
        // Silencioso: si fallan los roles, el filtro queda vacío
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