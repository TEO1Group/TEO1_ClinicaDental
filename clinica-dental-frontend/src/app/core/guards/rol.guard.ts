import { inject } from "@angular/core";
import { CanActivateFn, Router } from "@angular/router";
import { AuthService } from "../services/auth.service";

export const rolGuard =(rolesPermitidos: string[]): CanActivateFn => {
    return () => {
        const authService = inject(AuthService);
        const router = inject(Router);

        const rol = authService.obtenerRol();

        if (rol && rolesPermitidos.includes(rol)) {
            return true;
        }

        //si no tiene el rol, redirigir al login
        router.navigate(['']);
        return false;
    };
};