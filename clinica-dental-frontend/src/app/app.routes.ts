import { Routes } from '@angular/router';
import { FormularioRegistroClienteComponent } from './registro/components/formulario-registro/formulario-registro.component';
import { FormularioUsuarioSistemaComponent } from './registro-personal/formulario-usuario-sistema/formulario-usuario-sistema.component'; 
import { ListadoUsuariosComponent } from './registro-personal/listado-usuarios/listado-usuarios.component';
import { DashboardComponent } from './dashboard/dashboard.component';
import { LoginComponent } from './login/login.component';
import { ListadoDoctoresComponent } from './doctores/listado-doctores/listado-doctores.component';
import { DetalleDoctorComponent } from './doctores/detalle-doctor/detalle-doctor.component';
import { ListadoPacientesComponent } from './pacientes/components/listado-pacientes/listado-pacientes.component';
import { DetallePacienteComponent } from './pacientes/components/detalle-paciente/detalle-paciente.component';
import { FormularioPacienteComponent } from './pacientes/components/formulario-paciente/formulario-paciente.component';
import { FormularioListaNegraComponent } from './pacientes/components/formulario-lista-negra/formulario-lista-negra.component';
import { HistorialPacienteComponent } from './pacientes/components/historial-paciente/historial-paciente.component';
//guards
import { rolGuard } from './core/guards/rol.guard';

export const routes: Routes = [
  { path: '', component: LoginComponent },
  { path: 'registro', component: FormularioRegistroClienteComponent },
  { path: 'dashboard', component: DashboardComponent, canActivate: [rolGuard(['ADMIN', 'DOCTOR', 'SECRETARIA', 'CLIENTE'])]},
  { path: 'admin/usuarios', component: ListadoUsuariosComponent, canActivate: [rolGuard(['ADMIN'])]},
  { path: 'admin/usuarios/crear', component: FormularioUsuarioSistemaComponent, canActivate: [rolGuard(['ADMIN'])]},
  { path: 'doctores', component: ListadoDoctoresComponent, canActivate: [rolGuard(['ADMIN', 'DOCTOR', 'SECRETARIA'])]},
  { path: 'doctores/:id', component: DetalleDoctorComponent, canActivate: [rolGuard(['ADMIN', 'DOCTOR', 'SECRETARIA'])]},
  { path: 'pacientes', component: ListadoPacientesComponent, canActivate: [rolGuard(['ADMIN', 'DOCTOR', 'SECRETARIA'])]},
  { path: 'pacientes/:id/editar', component: FormularioPacienteComponent, canActivate: [rolGuard(['ADMIN', 'SECRETARIA'])]},
  { path: 'pacientes/:id/lista-negra', component: FormularioListaNegraComponent, canActivate: [rolGuard(['SECRETARIA'])]},
  { path: 'pacientes/:id/historial', component: HistorialPacienteComponent, canActivate: [rolGuard(['ADMIN', 'DOCTOR', 'SECRETARIA'])]},
  { path: 'pacientes/:id', component: DetallePacienteComponent, canActivate: [rolGuard(['ADMIN', 'DOCTOR', 'SECRETARIA'])]},
  { path: '**', redirectTo: '' }
];
