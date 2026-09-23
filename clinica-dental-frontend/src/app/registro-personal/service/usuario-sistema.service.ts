import { Injectable } from "@angular/core";
import { Observable } from "rxjs";
import { ApiService } from "../../core/services/api.service";
import { UsuarioSistemaRequest, UsuarioSistemaResponse } from "../models/usuario-sistema.model";
import { UsuarioListado } from "../models/usuario-listado.model";

export interface Rol {
    idRol: string;
    nombreRol: string;
    descripcion: string;
}

@Injectable({providedIn: 'root'})
export class UsuarioSistemaService extends ApiService {
  private readonly crearUsuarioUrl = `${this.baseUrl}/admin/usuarios`;
  private readonly rolesUrl = `${this.baseUrl}/roles`;
  private readonly listadoUrl = `${this.baseUrl}/admin/usuarios`;

  crearUsuario(request: UsuarioSistemaRequest): Observable<UsuarioSistemaResponse> {
    return this.http.post<UsuarioSistemaResponse>(this.crearUsuarioUrl, request);
  }

  obtenerRoles(): Observable<Rol[]> {
    return this.http.get<Rol[]>(this.rolesUrl);
  }

  listarUsuarios(): Observable<UsuarioListado[]> {
    return this.http.get<UsuarioListado[]>(this.listadoUrl);
  }
}