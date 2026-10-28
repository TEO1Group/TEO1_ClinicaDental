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
import { ListadoCitasComponent } from './citas/components/listado-citas/listado-citas.component';
import { FormularioCitaComponent } from './citas/components/formulario-cita/formulario-cita.component';
import { ListadoPlanesComponent } from './planes/components/listado-planes/listado-planes.component';
import { FormularioPlanComponent } from './planes/components/formulario-plan/formulario-plan.component';
import { DetallePlanComponent } from './planes/components/detalle-plan/detalle-plan.component';
import { ReporteCitasComponent } from './reportes/components/reporte-citas/reporte-citas.component';
import { MisTratamientosComponent } from './tratamientos-paciente/components/mis-tratamientos/mis-tratamientos.component';
import { ListadoTratamientosComponent } from './tratamientos-paciente/components/listado-tratamientos/listado-tratamientos.component';
import { MisPlanesComponent } from './planes/components/mis-planes/mis-planes.component';
//guards
import { rolGuard } from './core/guards/rol.guard';

export const routes: Routes = [
  { path: '', component: LoginComponent },
  { path: 'registro', component: FormularioRegistroClienteComponent },
  { path: 'dashboard', component: DashboardComponent, canActivate: [rolGuard(['ADMIN', 'DOCTOR', 'SECRETARIA', 'CLIENTE'])]},
  { path: 'admin/usuarios', component: ListadoUsuariosComponent, canActivate: [rolGuard(['ADMIN'])]},
  { path: 'admin/usuarios/crear', component: FormularioUsuarioSistemaComponent, canActivate: [rolGuard(['ADMIN'])]},
  { path: 'mis-tratamientos', component: MisTratamientosComponent, canActivate: [rolGuard(['CLIENTE'])]},
  { path: 'tratamientos', component: ListadoTratamientosComponent, canActivate: [rolGuard(['ADMIN'])]},
  { path: 'doctores', component: ListadoDoctoresComponent, canActivate: [rolGuard(['ADMIN', 'DOCTOR', 'SECRETARIA', 'CLIENTE'])]},
  { path: 'doctores/:id', component: DetalleDoctorComponent, canActivate: [rolGuard(['ADMIN', 'DOCTOR', 'SECRETARIA', 'CLIENTE'])]},
  { path: 'pacientes', component: ListadoPacientesComponent, canActivate: [rolGuard(['ADMIN', 'DOCTOR', 'SECRETARIA'])]},
  { path: 'pacientes/crear', component: FormularioRegistroClienteComponent, canActivate: [rolGuard(['ADMIN', 'SECRETARIA'])] },
  { path: 'pacientes/:id/editar', component: FormularioPacienteComponent, canActivate: [rolGuard(['ADMIN', 'SECRETARIA'])]},
  { path: 'pacientes/:id/lista-negra', component: FormularioListaNegraComponent, canActivate: [rolGuard(['SECRETARIA'])]},
  { path: 'pacientes/:id/historial', component: HistorialPacienteComponent, canActivate: [rolGuard(['ADMIN', 'DOCTOR', 'SECRETARIA'])]},
  { path: 'pacientes/:id', component: DetallePacienteComponent, canActivate: [rolGuard(['ADMIN', 'DOCTOR', 'SECRETARIA'])]},
  { path: 'citas', component: ListadoCitasComponent, canActivate: [rolGuard(['ADMIN', 'DOCTOR', 'SECRETARIA', 'CLIENTE'])]},
  { path: 'citas/nueva', component: FormularioCitaComponent, canActivate: [rolGuard(['CLIENTE', 'SECRETARIA', 'ADMIN'])]},
  { path: 'mis-planes', component: MisPlanesComponent, canActivate: [rolGuard(['CLIENTE'])]},
  { path: 'planes', component: ListadoPlanesComponent, canActivate: [rolGuard(['ADMIN', 'SECRETARIA', 'DOCTOR'])]},
  { path: 'planes/nuevo', component: FormularioPlanComponent, canActivate: [rolGuard(['ADMIN', 'SECRETARIA', 'DOCTOR'])]},
  { path: 'planes/:id', component: DetallePlanComponent, canActivate: [rolGuard(['ADMIN', 'SECRETARIA', 'DOCTOR'])]},
  { path: 'reportes', component: ReporteCitasComponent, canActivate: [rolGuard(['ADMIN', 'SECRETARIA'])]},
  { path: '**', redirectTo: '' }
];