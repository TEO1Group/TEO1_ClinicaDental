import { HttpInterceptorFn } from "@angular/common/http";
import { catchError } from "rxjs";
import { throwError } from "rxjs";

export const authInterceptor: HttpInterceptorFn = (req, next) => {
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
                console.log('No autorizado, redirigir al login');
            }
            if (err.status === 403) {
                console.log('Acceso prohibido');
            }
            return throwError(() => err);
        })
    );
};