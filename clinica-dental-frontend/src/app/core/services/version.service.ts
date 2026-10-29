import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { ApiService } from './api.service';

export interface VersionResponse {
  commit: string | null;
  fechaConstruccion: string | null;
}

@Injectable({ providedIn: 'root' })
export class VersionService extends ApiService {
  private readonly versionUrl = `${this.baseUrl}/version`;

  obtenerVersion(): Observable<VersionResponse | null> {
    return this.http.get<VersionResponse>(this.versionUrl).pipe(
      catchError(() => of(null))
    );
  }
}
