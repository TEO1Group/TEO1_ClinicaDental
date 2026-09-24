import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { UsuarioSistemaService, Rol } from '../service/usuario-sistema.service';
import { UsuarioListado } from '../models/usuario-listado.model';
import { NotificacionService } from '../../core/notificacion/service/notificacion.service';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-listado-usuarios',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './listado-usuarios.component.html',
  styleUrl: './listado-usuarios.component.scss'
})
export class ListadoUsuariosComponent implements OnInit {
  private readonly usuarioSistemaService = inject(UsuarioSistemaService);
  private readonly notificacionService = inject(NotificacionService);
  private readonly authService = inject(AuthService);
  private readonly formBuilder = inject(FormBuilder);

  readonly usuarioActual = this.authService.usuarioActual;
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

  // Modal editar
  mostrarModalEditar = false;
  isSubmittingEditar = false;
  usuarioEditando: UsuarioListado | null = null;

  readonly editarForm = this.formBuilder.nonNullable.group({
    nombre: ['', [Validators.maxLength(80)]],
    apellido: ['', [Validators.maxLength(80)]],
    email: ['', [Validators.email, Validators.maxLength(120)]],
    telefono: ['', [Validators.pattern(/^[0-9]{8}$/)]]
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

  // Modal editar
  abrirModalEditar(usuario: UsuarioListado): void {
    this.usuarioEditando = usuario;
    this.editarForm.patchValue({
      nombre: usuario.nombre,
      apellido: usuario.apellido,
      email: usuario.email,
      telefono: usuario.telefono || ''
    });
    this.mostrarModalEditar = true;
  }

  cerrarModalEditar(): void {
    this.mostrarModalEditar = false;
    this.usuarioEditando = null;
    this.editarForm.reset();
  }

  guardarEdicion(): void {
    if (this.editarForm.invalid || !this.usuarioEditando) {
      this.editarForm.markAllAsTouched();
      return;
    }

    this.isSubmittingEditar = true;

    const valores = this.editarForm.getRawValue();
    const request = {
      nombre: valores.nombre || undefined,
      apellido: valores.apellido || undefined,
      email: valores.email || undefined,
      telefono: valores.telefono || undefined
    };

    this.usuarioSistemaService.actualizarUsuario(this.usuarioEditando.idUsuario, request).subscribe({
      next: (response) => {
        this.isSubmittingEditar = false;
        this._usuarios.update(lista =>
          lista.map(u => u.idUsuario === response.idUsuario ? response : u)
        );
        this.notificacionService.exito('Usuario actualizado correctamente.');
        this.cerrarModalEditar();
      },
      error: (error) => {
        this.isSubmittingEditar = false;
        this.notificacionService.error(error.error?.mensaje || 'Error al actualizar el usuario.');
      }
    });
  }

  // Cambiar estado
  cambiarEstado(usuario: UsuarioListado): void {
    const actual = this.usuarioActual();
    if (actual && actual.idUsuario === usuario.idUsuario) {
      this.notificacionService.error('No puedes desactivar tu propia cuenta.');
      return;
    }

    const nuevoEstado = usuario.estado === 'ACTIVO' ? 'INACTIVO' : 'ACTIVO';
    const accion = nuevoEstado === 'ACTIVO' ? 'activar' : 'desactivar';

    if (!confirm(`¿Estás seguro de ${accion} a ${usuario.nombre} ${usuario.apellido}?`)) {
      return;
    }

    this.usuarioSistemaService.cambiarEstado(usuario.idUsuario, { estado: nuevoEstado }).subscribe({
      next: (response) => {
        this._usuarios.update(lista =>
          lista.map(u => u.idUsuario === response.idUsuario ? response : u)
        );
        this.notificacionService.exito(`Usuario ${nuevoEstado === 'ACTIVO' ? 'activado' : 'desactivado'} correctamente.`);
      },
      error: (error) => {
        this.notificacionService.error(error.error?.mensaje || 'Error al cambiar el estado del usuario.');
      }
    });
  }
}