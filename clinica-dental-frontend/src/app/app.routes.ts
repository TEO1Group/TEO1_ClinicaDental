import { Routes } from '@angular/router';
import { FormularioRegistroClienteComponent } from './registro/components/formulario-registro/formulario-registro.component';
import { LoginComponent } from './login/login.component';


export const routes: Routes = [
  { path: '', component: LoginComponent },
  { path: 'registro', component: FormularioRegistroClienteComponent },
  { path: '**', redirectTo: '' }
];
