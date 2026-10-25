import { Injectable, computed, signal } from '@angular/core';

import { Rol, Usuario } from '../models/modelos';

// Prototipo: usuarios de demostración en la vista. En la versión final el backend valida
// la contraseña (guardada con BCrypt) y devuelve un token JWT con el rol.
const USUARIOS_DEMO: (Usuario & { clave: string })[] = [
  { nombre: 'Administrador', usuario: 'admin', clave: 'admin123', rol: 'ADMINISTRADOR' },
  { nombre: 'Vendedor de turno', usuario: 'vendedor', clave: 'vendedor123', rol: 'VENDEDOR' },
];

const CLAVE_SESION = 'ferreteria.usuario';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly actual = signal<Usuario | null>(this.leerSesion());

  readonly usuario = this.actual.asReadonly();
  readonly esAdministrador = computed(() => this.actual()?.rol === 'ADMINISTRADOR');

  ingresar(usuario: string, clave: string): boolean {
    const encontrado = USUARIOS_DEMO.find((u) => u.usuario === usuario && u.clave === clave);
    if (!encontrado) {
      return false;
    }
    const { clave: _, ...datos } = encontrado;
    this.actual.set(datos);
    sessionStorage.setItem(CLAVE_SESION, JSON.stringify(datos));
    return true;
  }

  salir(): void {
    this.actual.set(null);
    sessionStorage.removeItem(CLAVE_SESION);
  }

  tieneRol(roles: Rol[]): boolean {
    const usuario = this.actual();
    return usuario !== null && roles.includes(usuario.rol);
  }

  private leerSesion(): Usuario | null {
    const guardado = sessionStorage.getItem(CLAVE_SESION);
    return guardado ? (JSON.parse(guardado) as Usuario) : null;
  }
}
