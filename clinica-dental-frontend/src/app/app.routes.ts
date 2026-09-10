import { Routes } from '@angular/router';
import { FormularioRegistroClienteComponent } from './registro/components/formulario-registro/formulario-registro.component';
import { FormularioUsuarioSistemaComponent } from './registro-personal/formulario-usuario-sistema/formulario-usuario-sistema.component'; 
import { DashboardComponent } from './dashboard/dashboard.component';
import { LoginComponent } from './login/login.component';
//guards
import { rolGuard } from './core/guards/rol.guard';


export const routes: Routes = [
  { path: 'dashboard', component: DashboardComponent},
  { path: '', component: LoginComponent },
  { path: 'registro', component: FormularioRegistroClienteComponent },
  { path: 'admin/usuarios/crear', component: FormularioUsuarioSistemaComponent/*, canActivate: [rolGuard(['ADMIN'])]*/},
  { path: '**', redirectTo: '' }
];
