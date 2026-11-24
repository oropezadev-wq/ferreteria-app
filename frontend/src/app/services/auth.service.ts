import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, map, tap } from 'rxjs';

import { API } from './api.service';
import { Rol, Usuario } from '../models/modelos';

interface Sesion extends Usuario {
  token: string;
}

const CLAVE_SESION = 'ferreteria.sesion';

/**
 * Inicio de sesión contra la API. El backend valida la contraseña (guardada con BCrypt)
 * y devuelve un token JWT con el rol; el token se envía en cada petición (ver auth.interceptor).
 * Se guarda en sessionStorage: al cerrar el navegador se cierra la sesión.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly actual = signal<Sesion | null>(this.leerSesion());

  readonly usuario = computed<Usuario | null>(() => {
    const sesion = this.actual();
    return sesion ? { nombre: sesion.nombre, usuario: sesion.usuario, rol: sesion.rol } : null;
  });
  readonly esAdministrador = computed(() => this.actual()?.rol === 'ADMINISTRADOR');

  ingresar(usuario: string, clave: string): Observable<void> {
    return this.http.post<Sesion>(`${API}/auth/login`, { usuario, clave }).pipe(
      tap((sesion) => {
        this.actual.set(sesion);
        sessionStorage.setItem(CLAVE_SESION, JSON.stringify(sesion));
      }),
      map(() => undefined),
    );
  }

  salir(): void {
    this.actual.set(null);
    sessionStorage.removeItem(CLAVE_SESION);
  }

  token(): string | null {
    return this.actual()?.token ?? null;
  }

  tieneRol(roles: Rol[]): boolean {
    const usuario = this.actual();
    return !!usuario && roles.includes(usuario.rol);
  }

  private leerSesion(): Sesion | null {
    const guardada = sessionStorage.getItem(CLAVE_SESION);
    return guardada ? (JSON.parse(guardada) as Sesion) : null;
  }
}
