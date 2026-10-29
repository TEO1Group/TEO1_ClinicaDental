import { Routes } from '@angular/router';
//guards
import { rolGuard } from './core/guards/rol.guard';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () => import('./login/login.component').then(m => m.LoginComponent)
  },
  {
    path: 'registro',
    loadComponent: () => import('./registro/components/formulario-registro/formulario-registro.component').then(m => m.FormularioRegistroClienteComponent)
  },
  {
    path: 'dashboard',
    loadComponent: () => import('./dashboard/dashboard.component').then(m => m.DashboardComponent),
    canActivate: [rolGuard(['ADMIN', 'DOCTOR', 'SECRETARIA', 'CLIENTE'])]
  },
  {
    path: 'admin/usuarios',
    loadComponent: () => import('./registro-personal/listado-usuarios/listado-usuarios.component').then(m => m.ListadoUsuariosComponent),
    canActivate: [rolGuard(['ADMIN'])]
  },
  {
    path: 'admin/usuarios/crear',
    loadComponent: () => import('./registro-personal/formulario-usuario-sistema/formulario-usuario-sistema.component').then(m => m.FormularioUsuarioSistemaComponent),
    canActivate: [rolGuard(['ADMIN'])]
  },
  {
    path: 'mis-tratamientos',
    loadComponent: () => import('./tratamientos-paciente/components/mis-tratamientos/mis-tratamientos.component').then(m => m.MisTratamientosComponent),
    canActivate: [rolGuard(['CLIENTE'])]
  },
  {
    path: 'tratamientos',
    loadComponent: () => import('./tratamientos-paciente/components/listado-tratamientos/listado-tratamientos.component').then(m => m.ListadoTratamientosComponent),
    canActivate: [rolGuard(['ADMIN'])]
  },
  {
    path: 'doctores',
    loadComponent: () => import('./doctores/listado-doctores/listado-doctores.component').then(m => m.ListadoDoctoresComponent),
    canActivate: [rolGuard(['ADMIN', 'DOCTOR', 'SECRETARIA', 'CLIENTE'])]
  },
  {
    path: 'doctores/:id',
    loadComponent: () => import('./doctores/detalle-doctor/detalle-doctor.component').then(m => m.DetalleDoctorComponent),
    canActivate: [rolGuard(['ADMIN', 'DOCTOR', 'SECRETARIA', 'CLIENTE'])]
  },
  {
    path: 'pacientes',
    loadComponent: () => import('./pacientes/components/listado-pacientes/listado-pacientes.component').then(m => m.ListadoPacientesComponent),
    canActivate: [rolGuard(['ADMIN', 'DOCTOR', 'SECRETARIA'])]
  },
  {
    path: 'pacientes/crear',
    loadComponent: () => import('./registro/components/formulario-registro/formulario-registro.component').then(m => m.FormularioRegistroClienteComponent),
    canActivate: [rolGuard(['ADMIN', 'SECRETARIA'])]
  },
  {
    path: 'pacientes/:id/editar',
    loadComponent: () => import('./pacientes/components/formulario-paciente/formulario-paciente.component').then(m => m.FormularioPacienteComponent),
    canActivate: [rolGuard(['ADMIN', 'SECRETARIA'])]
  },
  {
    path: 'pacientes/:id/lista-negra',
    loadComponent: () => import('./pacientes/components/formulario-lista-negra/formulario-lista-negra.component').then(m => m.FormularioListaNegraComponent),
    canActivate: [rolGuard(['SECRETARIA'])]
  },
  {
    path: 'pacientes/:id/historial',
    loadComponent: () => import('./pacientes/components/historial-paciente/historial-paciente.component').then(m => m.HistorialPacienteComponent),
    canActivate: [rolGuard(['ADMIN', 'DOCTOR', 'SECRETARIA'])]
  },
  {
    path: 'pacientes/:id',
    loadComponent: () => import('./pacientes/components/detalle-paciente/detalle-paciente.component').then(m => m.DetallePacienteComponent),
    canActivate: [rolGuard(['ADMIN', 'DOCTOR', 'SECRETARIA'])]
  },
  {
    path: 'citas',
    loadComponent: () => import('./citas/components/listado-citas/listado-citas.component').then(m => m.ListadoCitasComponent),
    canActivate: [rolGuard(['ADMIN', 'DOCTOR', 'SECRETARIA', 'CLIENTE'])]
  },
  {
    path: 'citas/nueva',
    loadComponent: () => import('./citas/components/formulario-cita/formulario-cita.component').then(m => m.FormularioCitaComponent),
    canActivate: [rolGuard(['CLIENTE', 'SECRETARIA', 'ADMIN'])]
  },
  {
    path: 'mis-planes',
    loadComponent: () => import('./planes/components/mis-planes/mis-planes.component').then(m => m.MisPlanesComponent),
    canActivate: [rolGuard(['CLIENTE'])]
  },
  {
    path: 'planes',
    loadComponent: () => import('./planes/components/listado-planes/listado-planes.component').then(m => m.ListadoPlanesComponent),
    canActivate: [rolGuard(['ADMIN', 'SECRETARIA', 'DOCTOR'])]
  },
  {
    path: 'planes/nuevo',
    loadComponent: () => import('./planes/components/formulario-plan/formulario-plan.component').then(m => m.FormularioPlanComponent),
    canActivate: [rolGuard(['ADMIN', 'SECRETARIA', 'DOCTOR'])]
  },
  {
    path: 'planes/:id',
    loadComponent: () => import('./planes/components/detalle-plan/detalle-plan.component').then(m => m.DetallePlanComponent),
    canActivate: [rolGuard(['ADMIN', 'SECRETARIA', 'DOCTOR'])]
  },
  {
    path: 'reportes',
    loadComponent: () => import('./reportes/components/reporte-citas/reporte-citas.component').then(m => m.ReporteCitasComponent),
    canActivate: [rolGuard(['ADMIN', 'SECRETARIA'])]
  },
  { path: '**', redirectTo: '' }
];