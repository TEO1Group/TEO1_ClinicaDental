import { inject } from "@angular/core";
import { CanActivateFn, Router } from "@angular/router";
import { AuthService } from "../services/auth.service";
import { NotificacionService } from "../notificacion/service/notificacion.service";

export const rolGuard = (rolesPermitidos: string[]): CanActivateFn => {
  return () => {
    const authService = inject(AuthService);
    const router = inject(Router);
    const notificacionService = inject(NotificacionService);

    const rol = authService.obtenerRol();

    // Tiene el rol permitido -> acceso
    if (rol && rolesPermitidos.includes(rol)) {
      return true;
    }

    // Autenticado pero sin permiso -> notificar y al dashboard
    if (rol) {
      notificacionService.error('No tienes permisos para acceder a esta sección.');
      router.navigate(['/dashboard']);
      return false;
    }

    // No autenticado -> al login
    router.navigate(['']);
    return false;
  };
};