import { HttpInterceptorFn } from "@angular/common/http";
import { catchError, throwError } from "rxjs";
import { inject } from "@angular/core";
import { Router } from "@angular/router";
import { AuthService } from "../services/auth.service";
import { NotificacionService } from "../notificacion/service/notificacion.service";

export const authInterceptor: HttpInterceptorFn = (req, next) => {
    const authService = inject(AuthService);
    const router = inject(Router);
    const notificacionService = inject(NotificacionService);

    const token = localStorage.getItem('token');

    if (token) {
        req = req.clone({
            setHeaders: {
                Authorization: `Bearer ${token}`
            }
        });
    }

    return next(req).pipe(
        catchError(err => {
            if (err.status === 401) {
                authService.limpiarSesion();
                notificacionService.error('Tu sesión expiró. Inicia sesión nuevamente.');
                router.navigate(['']);
            }
            if (err.status === 403) {
                notificacionService.error('No tienes permisos para acceder a esta sección.');
            }
            return throwError(() => err);
        })
    );
};